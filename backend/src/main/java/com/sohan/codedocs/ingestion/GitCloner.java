package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.config.properties.IngestionProperties;
import com.sohan.codedocs.exception.IngestionException;
import com.sohan.codedocs.ingestion.model.CloneResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.storage.file.WindowCacheConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitCloner {

    private final IngestionProperties props;

    /**
     * JGit memory-maps pack files by default. Windows refuses to delete a
     * mapped file, so the temp clone can never be cleaned up and %TEMP% grows
     * with every ingestion. Disabling mmap costs a little read speed and makes
     * cleanup work on every platform.
     */
    @PostConstruct
    void disablePackFileMapping() {
        WindowCacheConfig config = new WindowCacheConfig();
        config.setPackedGitMMAP(false);
        config.install();
    }

    public CloneResult shallowClone(String remoteUrl, String branch) {
        Path target = createWorkDir();
        try {
            CloneCommand command = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(target.toFile())
                    .setDepth(1)                   // current tree only, no history
                    .setCloneSubmodules(false)     // a submodule could point anywhere
                    .setNoTags()
                    .setTimeout((int) props.cloneTimeout().toSeconds());

            if (branch != null && !branch.isBlank()) {
                command.setBranch(branch);
            }

            try (Git git = command.call()) {
                Repository repository = git.getRepository();
                ObjectId head = repository.resolve("HEAD");
                if (head == null) {
                    throw new IngestionException("Repository has no commits");
                }
                return new CloneResult(target, head.getName(), repository.getBranch());
            }
        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            // Generic message on purpose: JGit exceptions echo the URL and local paths.
            log.warn("Clone failed for {}", remoteUrl, e);
            throw new IngestionException(
                    "Unable to clone the repository. Check that it is public "
                            + "and the branch exists.", e);
        }
    }

    private Path createWorkDir() {
        try {
            return Files.createTempDirectory("codedocs-");
        } catch (IOException e) {
            throw new IngestionException("Unable to allocate a working directory", e);
        }
    }
}
