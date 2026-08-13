package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.dto.request.CreateRepoRequest;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.enums.RepoStatus;
import com.sohan.codedocs.exception.ApiException;
import com.sohan.codedocs.exception.NotFoundException;
import com.sohan.codedocs.repository.IndexedRepoRepository;
import com.sohan.codedocs.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepoServiceImpl implements RepoService {

    private static final List<RepoStatus> IN_PROGRESS = List.of(
            RepoStatus.PENDING, RepoStatus.CLONING, RepoStatus.CHUNKING, RepoStatus.EMBEDDING);

    private final IndexedRepoRepository repoRepository;

    @Override
    @Transactional
    public IndexedRepo register(CreateRepoRequest request, UUID ownerId){
        String normalisedUrl = normalise(request.gitUrl());

        // Scoped per owner, not global: two different users indexing the
        // same public repo is normal and each gets their own copy, their
        // own chat threads.
        if (repoRepository.existsByRemoteUrlIgnoreCaseAndStatusInAndOwnerId(normalisedUrl, IN_PROGRESS, ownerId)) {
            throw new ApiException("This repository is already being indexed.");
        }

        IndexedRepo repo = new IndexedRepo();
        repo.setOwnerId(ownerId);
        repo.setRemoteUrl(normalisedUrl);
        repo.setName(extractName(normalisedUrl));
        repo.setBranch(request.branch());
        repo.setStatus(RepoStatus.PENDING);
        repo.setCreatedAt(Instant.now());
        return repoRepository.save(repo);
    }

    @Override
    @Transactional
    public IndexedRepo resetForRetry(UUID id, UUID ownerId) {
        IndexedRepo repo = findOrThrow(id, ownerId);
        if (repo.getStatus() != RepoStatus.FAILED) {
            throw new ApiException("Only failed repositories can be retried.");
        }
        repo.setStatus(RepoStatus.PENDING);
        repo.setErrorMessage(null);
        repo.setDocumentCount(0);
        repo.setChunkCount(0);
        return repoRepository.save(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public IndexedRepo prepareForSync(UUID id, UUID ownerId) {
        IndexedRepo repo = findOrThrow(id, ownerId);
        if (repo.getStatus() != RepoStatus.READY) {
            throw new ApiException("Only ready repositories can be synced.");
        }
        return repo;
    }

    @Override
    @Transactional(readOnly = true)
    public IndexedRepo findOrThrow(UUID id, UUID ownerId) {
        return repoRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexedRepo> findAll(UUID ownerId) {
        return repoRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID ownerId) {
        IndexedRepo repo = findOrThrow(id, ownerId);
        // Documents and chunks go with it via ON DELETE CASCADE
        repoRepository.delete(repo);
    }

    private String normalise(String url) {
        String cleaned = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return cleaned.endsWith(".git") ? cleaned.substring(0, cleaned.length() - 4) : cleaned;
    }

    private String extractName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
