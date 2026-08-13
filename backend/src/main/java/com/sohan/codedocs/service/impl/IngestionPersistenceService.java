package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.entity.CodeChunk;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.entity.SourceDocument;
import com.sohan.codedocs.enums.RepoStatus;
import com.sohan.codedocs.repository.CodeChunkRepository;
import com.sohan.codedocs.repository.IndexedRepoRepository;
import com.sohan.codedocs.repository.SourceDocumentRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionPersistenceService {

    private final IndexedRepoRepository repoRepository;
    private final SourceDocumentRepository documentRepository;
    private final CodeChunkRepository chunkRepository;

    @Transactional(readOnly = true)
    public IndexedRepo load(UUID repoId) {
        return repoRepository.findById(repoId).orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID repoId, RepoStatus status) {
        IndexedRepo repo = repoRepository.findById(repoId).orElseThrow();
        repo.setStatus(status);
        repoRepository.saveAndFlush(repo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID repoId, String message) {
        IndexedRepo repo = repoRepository.findById(repoId).orElseThrow();
        repo.setStatus(RepoStatus.FAILED);
        repo.setErrorMessage(truncate(message));
        repoRepository.saveAndFlush(repo);
    }

    @Transactional
    public void recordCloneMetadata(UUID repoId, String commitSha, String branch) {
        IndexedRepo repo = repoRepository.findById(repoId).orElseThrow();
        repo.setCommitSha(commitSha);
        repo.setBranch(branch);
        repoRepository.save(repo);
    }

    @Transactional
    public SourceDocument saveDocument(SourceDocument document) {
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<SourceDocument> loadDocuments(UUID repoId) {
        return documentRepository.findAllByRepoId(repoId);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        documentRepository.deleteById(documentId);   // chunks cascade
    }

    @Transactional
    public void saveChunkBatch(List<CodeChunk> chunks) {
        chunkRepository.saveAll(chunks);
    }

    @Transactional(readOnly = true)
    public long countChunks(UUID repoId) {
        return chunkRepository.countByDocumentRepoId(repoId);
    }

    @Transactional
    public void markReady(UUID repoId, int documentCount, int chunkCount,
                          String embeddingModel, int dimensions) {
        IndexedRepo repo = repoRepository.findById(repoId).orElseThrow();
        repo.setDocumentCount(documentCount);
        repo.setChunkCount(chunkCount);
        repo.setEmbeddingModel(embeddingModel);
        repo.setEmbeddingDimensions(dimensions);
        repo.setIndexedAt(Instant.now());
        repo.setStatus(RepoStatus.READY);
        // A prior sync failure's message must not linger once a later one
        // (or this) succeeds — nothing else on the success path clears it.
        repo.setErrorMessage(null);
        repoRepository.save(repo);
    }

    /**
     * Unlike markFailed, this leaves the repo READY and its existing index
     * untouched: a sync is a best-effort refresh, not a rebuild, so a
     * mid-sync failure should fall back to "still usable with the last
     * good index" rather than taking the repo down.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSyncFailure(UUID repoId, String message) {
        IndexedRepo repo = repoRepository.findById(repoId).orElseThrow();
        repo.setStatus(RepoStatus.READY);
        repo.setErrorMessage(truncate(message));
        repoRepository.saveAndFlush(repo);
    }

    @Transactional
    public void deletePartialData(UUID repoId) {
        documentRepository.deleteAllByRepoId(repoId);   // chunks cascade
    }

    private String truncate(String message) {
        if (message == null) return "Unknown error";
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }
}