package com.sohan.codedocs.service;

import java.util.UUID;

public interface IngestionService {
    void ingestAsync(UUID repoId);

    /** Incremental refresh of an already-indexed repo — see IngestionServiceImpl.syncAsync. */
    void syncAsync(UUID repoId);
}
