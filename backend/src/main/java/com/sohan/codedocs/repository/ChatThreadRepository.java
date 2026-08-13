package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    /**
     * A thread shows up under every repo it's linked to, not just its
     * primary one — a thread can span multiple repos now, so this joins
     * through chat_thread_repos rather than matching the repo_id column
     * directly.
     */
    @Query("""
            SELECT t FROM ChatThread t
            WHERE t.ownerId = :ownerId
              AND t.id IN (SELECT ctr.threadId FROM ChatThreadRepo ctr WHERE ctr.repoId = :repoId)
            ORDER BY t.updatedAt DESC
            """)
    List<ChatThread> findAllByRepoIdAndOwnerIdOrderByUpdatedAtDesc(
            @Param("repoId") UUID repoId, @Param("ownerId") UUID ownerId);

    Optional<ChatThread> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Deliberately no owner check — this is the public share-link lookup. */
    Optional<ChatThread> findByShareToken(String shareToken);
}
