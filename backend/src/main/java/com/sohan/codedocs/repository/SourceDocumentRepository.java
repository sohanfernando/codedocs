package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, UUID> {
    void deleteAllByRepoId(UUID repoId);

    /** Used to diff against a fresh scan during an incremental sync. */
    List<SourceDocument> findAllByRepoId(UUID repoId);
}
