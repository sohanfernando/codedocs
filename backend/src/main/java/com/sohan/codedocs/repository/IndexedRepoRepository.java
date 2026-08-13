package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.enums.RepoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndexedRepoRepository extends JpaRepository<IndexedRepo, UUID> {

    Optional<IndexedRepo> findByRemoteUrlIgnoreCaseAndStatus(String remoteUrl, RepoStatus status);

    List<IndexedRepo> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<IndexedRepo> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByRemoteUrlIgnoreCaseAndStatusInAndOwnerId(
            String remoteUrl, List<RepoStatus> statuses, UUID ownerId);

    /** Used by StuckIngestionMonitor to find ingestions that never reached a terminal status. */
    List<IndexedRepo> findAllByStatusInAndCreatedAtBefore(List<RepoStatus> statuses, Instant cutoff);
}
