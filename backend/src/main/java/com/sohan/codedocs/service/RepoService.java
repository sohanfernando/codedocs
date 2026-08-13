package com.sohan.codedocs.service;

import com.sohan.codedocs.dto.request.CreateRepoRequest;
import com.sohan.codedocs.entity.IndexedRepo;

import java.util.List;
import java.util.UUID;

public interface RepoService {
    IndexedRepo register(CreateRepoRequest request, UUID ownerId);

    IndexedRepo resetForRetry(UUID id, UUID ownerId);

    /** Validates the repo is READY and owned by the caller; does not itself change any state. */
    IndexedRepo prepareForSync(UUID id, UUID ownerId);

    /** Throws NotFoundException for both "doesn't exist" and "not yours" — never reveals which. */
    IndexedRepo findOrThrow(UUID id, UUID ownerId);

    List<IndexedRepo> findAll(UUID ownerId);

    void delete(UUID id, UUID ownerId);
}
