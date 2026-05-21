package com.repoinsight.service.source;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.RepoSource;
import com.repoinsight.util.PathUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

/**
 * Handles remote Git repository sources by cloning them locally.
 */
@Service
public class RemoteSourceService {

    private static final Logger log = LoggerFactory.getLogger(RemoteSourceService.class);

    private final AppProperties properties;

    public RemoteSourceService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Clones a remote repository and returns an enriched {@link RepoSource}.
     *
     * @param remoteUrl the Git URL
     * @param branch    optional branch name; {@code null} for default branch
     * @return a populated RepoSource
     */
    public RepoSource cloneAndResolve(String remoteUrl, String branch) {
        RepoSource source = new RepoSource(RepoSource.SourceType.REMOTE, remoteUrl);
        source.setId(PathUtil.newId());
        source.setRemoteUrl(remoteUrl);
        source.setBranch(branch);

        if (remoteUrl == null || remoteUrl.isBlank()) {
            source.setValid(false);
            source.setValidationError("Remote URL must not be empty");
            return source;
        }

        if (!PathUtil.isGitUrl(remoteUrl)) {
            source.setValid(false);
            source.setValidationError("Does not look like a valid Git URL: " + remoteUrl);
            return source;
        }

        String projectName = PathUtil.inferProjectName(remoteUrl);
        source.setResolvedName(projectName);

        // Prepare workspace directory
        Path workspaceBase = Paths.get(properties.getWorkspace().getBaseDir());
        Path cloneTarget = workspaceBase.resolve(source.getId() + "_" + projectName);

        try {
            Files.createDirectories(workspaceBase);

            log.info("Cloning {} into {}", remoteUrl, cloneTarget);

            var cloneCommand = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(cloneTarget.toFile())
                    .setDepth(1)    // shallow clone for speed
                    .setCloneAllBranches(false);

            if (branch != null && !branch.isBlank()) {
                cloneCommand.setBranch(branch);
            }

            try (Git git = cloneCommand.call()) {
                log.info("Clone complete: {}", cloneTarget);
            }

            source.setPath(cloneTarget.toAbsolutePath().toString());
            source.setValid(true);

            // Measure size
            try (var stream = Files.walk(cloneTarget)) {
                long[] counts = {0, 0};
                stream.filter(Files::isRegularFile).forEach(f -> {
                    counts[0]++;
                    try { counts[1] += Files.size(f); } catch (Exception ignored) {}
                });
                source.setTotalFiles((int) counts[0]);
                source.setFileSizeBytes(counts[1]);
            }

            log.info("Resolved remote source: {} ({} files)", projectName, source.getTotalFiles());

        } catch (GitAPIException e) {
            log.error("Failed to clone {}: {}", remoteUrl, e.getMessage());
            source.setValid(false);
            source.setValidationError("Git clone failed: " + e.getMessage());
            cleanup(cloneTarget);
        } catch (IOException e) {
            log.error("IO error while cloning {}: {}", remoteUrl, e.getMessage());
            source.setValid(false);
            source.setValidationError("IO error: " + e.getMessage());
            cleanup(cloneTarget);
        }

        return source;
    }

    private void cleanup(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                  .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
