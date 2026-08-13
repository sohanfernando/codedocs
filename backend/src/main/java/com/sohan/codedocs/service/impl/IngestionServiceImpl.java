package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.config.AsyncConfig;
import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.entity.CodeChunk;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.entity.SourceDocument;
import com.sohan.codedocs.enums.EmbeddingTaskType;
import com.sohan.codedocs.enums.RepoStatus;
import com.sohan.codedocs.ingestion.ChunkerFactory;
import com.sohan.codedocs.ingestion.FileScanner;
import com.sohan.codedocs.ingestion.GitCloner;
import com.sohan.codedocs.ingestion.model.CloneResult;
import com.sohan.codedocs.ingestion.model.RawChunk;
import com.sohan.codedocs.ingestion.model.ScannedFile;
import com.sohan.codedocs.service.EmbeddingClient;
import com.sohan.codedocs.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionServiceImpl implements IngestionService {

    private final IngestionPersistenceService persistence;
    private final GitCloner cloner;
    private final FileScanner scanner;
    private final ChunkerFactory chunkerFactory;
    private final EmbeddingClient embeddingClient;
    private final GeminiProperties geminiProperties;

    /**
     * Deliberately NOT @Transactional — see IngestionPersistenceService.
     * This method is the sole owner of RepoStatus transitions.
     */
    @Override
    @Async(AsyncConfig.INGESTION_EXECUTOR)
    public void ingestAsync(UUID repoId) {
        Path workDir = null;
        long startedAt = System.currentTimeMillis();

        try {
            IndexedRepo repo = persistence.load(repoId);

            persistence.updateStatus(repoId, RepoStatus.CLONING);
            CloneResult clone = cloner.shallowClone(repo.getRemoteUrl(), repo.getBranch());
            workDir = clone.path();
            persistence.recordCloneMetadata(repoId, clone.commitSha(), clone.branch());

            persistence.updateStatus(repoId, RepoStatus.CHUNKING);
            List<ScannedFile> files = scanner.scan(workDir);
            if (files.isEmpty()) {
                persistence.markFailed(repoId, "No indexable source files found.");
                return;
            }
            files = withManifest(files);

            persistence.updateStatus(repoId, RepoStatus.EMBEDDING);
            int chunkCount = embedAndPersist(repoId, files);
            if (chunkCount == 0) {
                persistence.markFailed(repoId, "No content could be indexed.");
                return;
            }

            persistence.markReady(repoId, files.size(), chunkCount,
                    embeddingClient.modelId(), embeddingClient.dimensions());

            log.info("Indexed repo {} — {} files, {} chunks in {} ms",
                    repoId, files.size(), chunkCount, System.currentTimeMillis() - startedAt);

        } catch (Exception ex) {
            log.error("Ingestion failed for repo {}", repoId, ex);
            safelyMarkFailed(repoId, ex);
        } finally {
            cleanUp(workDir);
        }
    }

    /**
     * Re-clones and re-scans like ingestAsync, but only re-chunks and
     * re-embeds files whose content actually changed since the last index —
     * see syncAndPersist. Only valid on a repo that's already READY
     * (RepoServiceImpl.prepareForSync enforces that before this runs), so
     * unlike ingestAsync a failure here must not touch the existing data:
     * see IngestionPersistenceService.recordSyncFailure.
     */
    @Override
    @Async(AsyncConfig.INGESTION_EXECUTOR)
    public void syncAsync(UUID repoId) {
        Path workDir = null;
        long startedAt = System.currentTimeMillis();

        try {
            IndexedRepo repo = persistence.load(repoId);

            persistence.updateStatus(repoId, RepoStatus.CLONING);
            CloneResult clone = cloner.shallowClone(repo.getRemoteUrl(), repo.getBranch());
            workDir = clone.path();

            persistence.updateStatus(repoId, RepoStatus.CHUNKING);
            List<ScannedFile> files = scanner.scan(workDir);
            if (files.isEmpty()) {
                persistence.recordSyncFailure(repoId, "No indexable source files found.");
                return;
            }
            files = withManifest(files);

            persistence.updateStatus(repoId, RepoStatus.EMBEDDING);
            SyncStats stats = syncAndPersist(repoId, files);

            persistence.recordCloneMetadata(repoId, clone.commitSha(), clone.branch());
            persistence.markReady(repoId, files.size(), (int) persistence.countChunks(repoId),
                    embeddingClient.modelId(), embeddingClient.dimensions());

            log.info("Synced repo {} — {} unchanged, {} updated, {} added, {} removed, {} ms",
                    repoId, stats.unchanged(), stats.updated(), stats.added(), stats.removed(),
                    System.currentTimeMillis() - startedAt);

        } catch (Exception ex) {
            log.error("Sync failed for repo {}", repoId, ex);
            try {
                persistence.recordSyncFailure(repoId, ex.getMessage());
            } catch (Exception nested) {
                log.error("Could not record sync failure for repo {}", repoId, nested);
            }
        } finally {
            cleanUp(workDir);
        }
    }

    /**
     * Streams file-by-file: chunks are embedded and written in batches rather
     * than accumulating every chunk of a large repository in heap first.
     */
    private int embedAndPersist(UUID repoId, List<ScannedFile> files) {
        IndexedRepo repo = persistence.load(repoId);
        int batchSize = geminiProperties.embeddingBatchSize();
        List<CodeChunk> pending = new ArrayList<>(batchSize);
        int total = 0;

        for (ScannedFile file : files) {
            SourceDocument document = persistence.saveDocument(toDocument(repo, file));

            for (RawChunk raw : chunkerFactory.forFile(file).chunk(file)) {
                pending.add(toChunk(document, raw));
                if (pending.size() >= batchSize) {
                    total += flush(pending);
                    pending.clear();
                }
            }
        }
        if (!pending.isEmpty()) {
            total += flush(pending);
        }
        return total;
    }

    /**
     * Same shape as embedAndPersist, except a file whose content hash
     * matches what's already stored is skipped entirely — no re-chunking,
     * no re-embedding, no API cost — and a file that disappeared from the
     * scan has its document (and chunks, via cascade) deleted.
     */
    private SyncStats syncAndPersist(UUID repoId, List<ScannedFile> files) {
        IndexedRepo repo = persistence.load(repoId);
        Map<String, SourceDocument> existingByPath = new HashMap<>();
        for (SourceDocument doc : persistence.loadDocuments(repoId)) {
            existingByPath.put(doc.getFilePath(), doc);
        }

        Set<String> scannedPaths = new HashSet<>();
        int batchSize = geminiProperties.embeddingBatchSize();
        List<CodeChunk> pending = new ArrayList<>(batchSize);
        int unchanged = 0, updated = 0, added = 0;

        for (ScannedFile file : files) {
            scannedPaths.add(file.relativePath());
            String hash = contentHash(file);
            SourceDocument existing = existingByPath.get(file.relativePath());

            if (existing != null && hash.equals(existing.getContentSha())) {
                unchanged++;
                continue;
            }

            if (existing != null) {
                persistence.deleteDocument(existing.getId());
                updated++;
            } else {
                added++;
            }

            SourceDocument document = toDocument(repo, file);
            document.setContentSha(hash);
            document = persistence.saveDocument(document);

            for (RawChunk raw : chunkerFactory.forFile(file).chunk(file)) {
                pending.add(toChunk(document, raw));
                if (pending.size() >= batchSize) {
                    flush(pending);
                    pending.clear();
                }
            }
        }
        if (!pending.isEmpty()) {
            flush(pending);
        }

        // Whatever's left in existingByPath's keys but never showed up in
        // this scan no longer exists in the repo.
        int removed = 0;
        for (Map.Entry<String, SourceDocument> entry : existingByPath.entrySet()) {
            if (!scannedPaths.contains(entry.getKey())) {
                persistence.deleteDocument(entry.getValue().getId());
                removed++;
            }
        }

        return new SyncStats(unchanged, updated, added, removed);
    }

    private int flush(List<CodeChunk> batch) {
        List<String> inputs = batch.stream().map(this::buildEmbeddingInput).toList();
        List<float[]> vectors =
                embeddingClient.embedBatch(inputs, EmbeddingTaskType.RETRIEVAL_DOCUMENT);

        for (int i = 0; i < batch.size(); i++) {
            batch.get(i).setEmbedding(vectors.get(i));
        }
        persistence.saveChunkBatch(List.copyOf(batch));
        return batch.size();
    }

    /**
     * Structural context is prepended before embedding only; the raw content is
     * what gets stored. The file path carries real semantic signal — "where is
     * auth handled" matches src/security/AuthService.java on the path alone,
     * even when the code inside never uses the word "authentication".
     */
    private String buildEmbeddingInput(CodeChunk chunk) {
        SourceDocument document = chunk.getDocument();
        return """
                File: %s (lines %d-%d)
                Language: %s
                ---
                %s""".formatted(document.getFilePath(), chunk.getStartLine(),
                chunk.getEndLine(), document.getLanguage(), chunk.getContent());
    }

    private SourceDocument toDocument(IndexedRepo repo, ScannedFile file) {
        SourceDocument document = new SourceDocument();
        document.setRepo(repo);
        document.setFilePath(file.relativePath());
        document.setLanguage(file.language());
        document.setContentSha(contentHash(file));
        return document;
    }

    private CodeChunk toChunk(SourceDocument document, RawChunk raw) {
        CodeChunk chunk = new CodeChunk();
        chunk.setDocument(document);
        chunk.setContent(raw.content());
        chunk.setStartLine(raw.startLine());
        chunk.setEndLine(raw.endLine());
        chunk.setTokenCount(raw.content().length() / 4);   // rough estimate
        return chunk;
    }

    /** Identifies whether a file's content changed since the last index — see syncAndPersist. */
    private String contentHash(ScannedFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.join("\n", file.lines()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is guaranteed available on every JVM", e);
        }
    }

    /** Failure bookkeeping must never itself throw and swallow the real cause. */
    private void safelyMarkFailed(UUID repoId, Exception cause) {
        try {
            persistence.deletePartialData(repoId);
            persistence.markFailed(repoId, cause.getMessage());
        } catch (Exception nested) {
            log.error("Could not mark repo {} as FAILED", repoId, nested);
        }
    }

    /**
     * Retries because file handles are sometimes released a moment after the
     * clone closes. If deletion still fails, the JVM cleans up on exit rather
     * than leaving the directory behind permanently.
     */
    private void cleanUp(Path workDir) {
        if (workDir == null) return;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                FileSystemUtils.deleteRecursively(workDir);
                return;
            } catch (Exception e) {
                if (attempt == 3) {
                    log.warn("Could not delete working directory {}", workDir, e);
                    workDir.toFile().deleteOnExit();
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * A synthetic document listing every indexed path.
     *
     * Retrieval only ever sees top-k chunks, so questions about the repository
     * as a whole ("what controllers exist?", "what is the project structure?")
     * cannot be answered from scattered code chunks. This gives them a single
     * chunk that holds the complete answer.
     */
    private List<ScannedFile> withManifest(List<ScannedFile> files) {
        List<String> lines = new ArrayList<>();
        lines.add("# Repository structure");
        lines.add("");
        lines.add("This repository contains " + files.size() + " indexed files:");
        lines.add("");
        files.stream()
                .map(ScannedFile::relativePath)
                .sorted()
                .forEach(path -> lines.add("- " + path));

        ScannedFile manifest = new ScannedFile(
                "REPOSITORY_STRUCTURE.md", "md", "markdown", lines);

        List<ScannedFile> combined = new ArrayList<>(files);
        combined.add(manifest);
        return combined;
    }

    private record SyncStats(int unchanged, int updated, int added, int removed) {}
}
