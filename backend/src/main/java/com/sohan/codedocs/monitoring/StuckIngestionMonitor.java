package com.sohan.codedocs.monitoring;

import com.sohan.codedocs.config.properties.IngestionProperties;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.enums.RepoStatus;
import com.sohan.codedocs.repository.IndexedRepoRepository;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Flags repositories that have sat in a non-terminal {@link RepoStatus} for
 * longer than {@code ingestion.stuck-after} — a crashed worker, a hung
 * Gemini call, or a pod restart mid-job can otherwise leave a repo
 * "Indexing…" forever with nothing surfacing it except a user eventually
 * noticing it never finished.
 *
 * <p>Detection only: this never mutates repo state itself. Auto-marking a
 * slow-but-healthy run as FAILED risks racing it, and retrying only makes
 * sense once someone has looked at why it actually stalled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckIngestionMonitor {

    private static final List<RepoStatus> IN_PROGRESS = List.of(
            RepoStatus.PENDING, RepoStatus.CLONING, RepoStatus.CHUNKING, RepoStatus.EMBEDDING);

    private final IndexedRepoRepository repoRepository;
    private final IngestionProperties ingestionProperties;

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void checkForStuckRepos() {
        Instant cutoff = Instant.now().minus(ingestionProperties.stuckAfter());
        List<IndexedRepo> stuck = repoRepository.findAllByStatusInAndCreatedAtBefore(IN_PROGRESS, cutoff);

        for (IndexedRepo repo : stuck) {
            Duration age = Duration.between(repo.getCreatedAt(), Instant.now());
            String message = "Ingestion stuck: repo '%s' [%s] has been %s for %s"
                    .formatted(repo.getName(), repo.getId(), repo.getStatus(), formatAge(age));
            log.warn(message);
            Sentry.captureMessage(message, SentryLevel.WARNING);
        }
    }

    private String formatAge(Duration age) {
        long hours = age.toHours();
        return hours > 0 ? hours + "h" + (age.toMinutesPart()) + "m" : age.toMinutes() + "m";
    }
}
