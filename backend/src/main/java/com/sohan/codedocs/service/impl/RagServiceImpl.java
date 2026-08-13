package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.dto.request.ChatRequest;
import com.sohan.codedocs.dto.response.ChatResponse;
import com.sohan.codedocs.dto.response.SourceRef;
import com.sohan.codedocs.entity.ChatThread;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.enums.EmbeddingTaskType;
import com.sohan.codedocs.enums.RepoStatus;
import com.sohan.codedocs.exception.ApiException;
import com.sohan.codedocs.repository.CodeChunkRepository;
import com.sohan.codedocs.repository.projection.ChunkSearchResult;
import com.sohan.codedocs.enums.ChatRole;
import com.sohan.codedocs.service.ChatClient;
import com.sohan.codedocs.service.ChatStreamListener;
import com.sohan.codedocs.service.ChatThreadService;
import com.sohan.codedocs.service.ChatTurn;
import com.sohan.codedocs.service.EmbeddingClient;
import com.sohan.codedocs.service.RagService;
import com.sohan.codedocs.service.RepoService;
import com.sohan.codedocs.util.PromptBuilder;
import com.sohan.codedocs.util.SourceLinkBuilder;
import com.sohan.codedocs.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final int TOP_K = 15;

    /**
     * Vector search always returns k results, however irrelevant. Without a
     * floor, "what's the weather?" returns eight random chunks that the model
     * then dutifully tries to answer from.
     */
    private static final double MIN_SIMILARITY = 0.30;

    /** Last 3 exchanges — enough for the model to resolve a follow-up without bloating every prompt. */
    private static final int HISTORY_LIMIT = 6;

    private final RepoService repoService;
    private final ChatThreadService chatThreadService;
    private final EmbeddingClient embeddingClient;
    private final ChatClient chatClient;
    private final CodeChunkRepository chunkRepository;
    private final PromptBuilder promptBuilder;
    private final SourceLinkBuilder sourceLinkBuilder;

    /**
     * Not @Transactional: this writes chat history via ChatThreadService
     * (each call its own short transaction — see there) alongside the plain
     * reads in retrieve(), which already carry their own default
     * transactions through Spring Data. Nothing here needs a wrapping one.
     */
    @Override
    public ChatResponse answer(ChatRequest request, UUID ownerId) {
        ChatThread thread = chatThreadService.findOrThrow(request.threadId(), ownerId);
        Retrieval retrieval = retrieve(thread, request.question(), ownerId);

        chatThreadService.appendUserMessage(thread.getId(), request.question());

        if (retrieval.noContext()) {
            String answer = ChatResponse.noContext().answer();
            chatThreadService.appendAssistantMessage(thread.getId(), answer, List.of(), false);
            return ChatResponse.noContext();
        }

        try {
            String answer = chatClient.complete(PromptBuilder.SYSTEM_PROMPT, retrieval.history(), retrieval.userPrompt());
            chatThreadService.appendAssistantMessage(thread.getId(), answer, retrieval.sources(), false);
            return new ChatResponse(answer, retrieval.sources());
        } catch (RuntimeException ex) {
            // No partial text is possible in blocking mode — this just
            // records that the attempt happened and failed.
            chatThreadService.appendAssistantMessage(thread.getId(), "", retrieval.sources(), true);
            throw ex;
        }
    }

    /**
     * Deliberately no surrounding transaction: this spans a blocking Gemini
     * call that can run for many seconds while tokens stream out, and a DB
     * transaction (and its pooled connection) has no business staying open
     * for that. retrieve() below runs correctly without one — see its javadoc.
     */
    @Override
    public void answerStream(ChatRequest request, UUID ownerId, ChatStreamListener listener) {
        ChatThread thread;
        Retrieval retrieval;
        try {
            thread = chatThreadService.findOrThrow(request.threadId(), ownerId);
            retrieval = retrieve(thread, request.question(), ownerId);
        } catch (Exception ex) {
            listener.onError(ex);
            return;
        }

        chatThreadService.appendUserMessage(thread.getId(), request.question());
        listener.onSources(retrieval.sources());

        if (retrieval.noContext()) {
            String answer = ChatResponse.noContext().answer();
            UUID messageId = chatThreadService.appendAssistantMessage(thread.getId(), answer, List.of(), false);
            listener.onToken(answer);
            listener.onComplete(messageId);
            return;
        }

        StringBuilder streamed = new StringBuilder();
        try {
            chatClient.completeStream(PromptBuilder.SYSTEM_PROMPT, retrieval.history(), retrieval.userPrompt(), token -> {
                streamed.append(token);
                listener.onToken(token);
            });
            UUID messageId = chatThreadService.appendAssistantMessage(
                    thread.getId(), streamed.toString(), retrieval.sources(), false);
            listener.onComplete(messageId);
        } catch (Exception ex) {
            // Whatever streamed before the failure is worth keeping — same
            // "don't wipe partial content" principle the frontend applies.
            if (!streamed.isEmpty()) {
                chatThreadService.appendAssistantMessage(
                        thread.getId(), streamed.toString(), retrieval.sources(), true);
            }
            listener.onError(ex);
        }
    }

    /**
     * Shared validation + vector search + prompt assembly behind both
     * answer() and answerStream(). A thread can span more than one repo —
     * every repo in the set must be READY and on the same embedding model
     * before anything is searched, and each hit is matched back to its own
     * repo (not just "the" repo) when building its citation.
     *
     * Not @Transactional itself — it relies on the caller for that when one
     * is needed. Safe either way: IndexedRepo has no lazy associations and
     * ChunkSearchResult is a flat native-query projection, so nothing here
     * requires a still-open Hibernate session to read afterward.
     */
    private Retrieval retrieve(ChatThread thread, String question, UUID ownerId) {
        List<UUID> repoIds = chatThreadService.repoIds(thread.getId(), ownerId);
        List<IndexedRepo> repos = repoIds.stream()
                .map(repoId -> repoService.findOrThrow(repoId, ownerId))
                .toList();

        for (IndexedRepo repo : repos) {
            // Status first: a FAILED or in-progress repo has null model metadata,
            // so the mismatch check below would fail before this guard could run.
            if (repo.getStatus() != RepoStatus.READY) {
                throw new ApiException("\"%s\" is not ready yet (status: %s)."
                        .formatted(repo.getName(), repo.getStatus()));
            }
            // Vectors written by a different model are not comparable to the
            // query vector. Objects.equals avoids unboxing a null Integer.
            if (!embeddingClient.modelId().equals(repo.getEmbeddingModel())
                    || !Objects.equals(embeddingClient.dimensions(), repo.getEmbeddingDimensions())) {
                throw new ApiException("\"%s\" was indexed with a different embedding model and must be re-indexed."
                        .formatted(repo.getName()));
            }
        }

        Map<UUID, IndexedRepo> reposById = repos.stream()
                .collect(Collectors.toMap(IndexedRepo::getId, r -> r));

        // RETRIEVAL_QUERY here, RETRIEVAL_DOCUMENT during ingestion:
        // same model, different optimisation for each side of the search.
        float[] questionVector =
                embeddingClient.embed(question, EmbeddingTaskType.RETRIEVAL_QUERY);

        List<ChunkSearchResult> hits = chunkRepository.searchByEmbedding(
                repoIds, VectorUtils.toPgVectorLiteral(questionVector), TOP_K);

        List<ChunkSearchResult> relevant = hits.stream()
                .filter(hit -> hit.getSimilarity() >= MIN_SIMILARITY)
                .toList();

        List<ChatTurn> history = buildHistory(thread.getId());

        if (relevant.isEmpty()) {
            log.debug("No chunk above similarity floor for thread {}", thread.getId());
            return new Retrieval(List.of(), null, history, true);
        }

        // Index i+1 must match the [n] numbering PromptBuilder used, or the
        // citations in the answer text point at the wrong files. Each hit is
        // attributed to its own repo — the source of a citation link and
        // commit pin, not just whichever repo happened to be selected first.
        List<SourceRef> sources = new ArrayList<>(relevant.size());
        for (int i = 0; i < relevant.size(); i++) {
            ChunkSearchResult hit = relevant.get(i);
            sources.add(sourceLinkBuilder.toSourceRef(reposById.get(hit.getRepoId()), hit, i + 1));
        }

        String userPrompt = promptBuilder.buildUserPrompt(question, relevant);
        return new Retrieval(sources, userPrompt, history, false);
    }

    /**
     * The thread's own prior messages, oldest first — retrieve() runs before
     * the current question is persisted, so "everything in the thread right
     * now" already is exactly the history that belongs before it. Empty
     * (failed-with-no-text) messages are dropped; they add noise, not context.
     */
    private List<ChatTurn> buildHistory(UUID threadId) {
        return chatThreadService.recentMessages(threadId, HISTORY_LIMIT).stream()
                .filter(m -> !m.content().isBlank())
                .map(m -> m.role() == ChatRole.USER ? ChatTurn.user(m.content()) : ChatTurn.model(m.content()))
                .toList();
    }

    private record Retrieval(List<SourceRef> sources, String userPrompt, List<ChatTurn> history, boolean noContext) {}
}
