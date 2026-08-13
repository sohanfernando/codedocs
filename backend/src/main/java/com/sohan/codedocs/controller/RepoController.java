package com.sohan.codedocs.controller;

import com.sohan.codedocs.dto.request.CreateRepoRequest;
import com.sohan.codedocs.dto.response.RepoResponse;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.security.AppUserDetails;
import com.sohan.codedocs.service.IngestionService;
import com.sohan.codedocs.service.RepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;
    private final IngestionService ingestionService;

    /**
     * 202 Accepted, not 201: indexing has been queued, not completed.
     */
    @PostMapping
    public ResponseEntity<RepoResponse> create(@Valid @RequestBody CreateRepoRequest request,
                                               @AuthenticationPrincipal AppUserDetails principal,
                                               UriComponentsBuilder uriBuilder) {
        IndexedRepo repo = repoService.register(request, principal.id());
        ingestionService.ingestAsync(repo.getId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(uriBuilder.path("/api/repositories/{id}")
                        .buildAndExpand(repo.getId()).toUri())
                .body(RepoResponse.from(repo));
    }

    /**
     * Re-runs ingestion for a repo that previously failed. Partial data from the
     * failed run is already cleaned up, so this starts fresh.
     */
    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RepoResponse retry(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        IndexedRepo repo = repoService.resetForRetry(id, principal.id());
        ingestionService.ingestAsync(repo.getId());
        return RepoResponse.from(repo);
    }

    /**
     * Incremental refresh of an already-indexed repo: unlike /retry this
     * only re-embeds files that actually changed. 202 for the same reason
     * as create() — the sync has been queued, not completed.
     */
    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RepoResponse sync(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        IndexedRepo repo = repoService.prepareForSync(id, principal.id());
        ingestionService.syncAsync(repo.getId());
        return RepoResponse.from(repo);
    }

    @GetMapping("/{id}")
    public RepoResponse get(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return RepoResponse.from(repoService.findOrThrow(id, principal.id()));
    }

    @GetMapping
    public List<RepoResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
        return repoService.findAll(principal.id()).stream().map(RepoResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        repoService.delete(id, principal.id());
    }
}
