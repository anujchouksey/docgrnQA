package com.repoinsight.service.source;

import com.repoinsight.dto.AnalysisRequestDto;
import com.repoinsight.model.RepoSource;
import org.springframework.stereotype.Service;

/**
 * Facade that delegates to {@link LocalSourceService} or {@link RemoteSourceService}
 * based on the analysis request.
 */
@Service
public class SourceIngestionService {

    private final LocalSourceService  localService;
    private final RemoteSourceService remoteService;

    public SourceIngestionService(LocalSourceService localService, RemoteSourceService remoteService) {
        this.localService  = localService;
        this.remoteService = remoteService;
    }

    /** Resolves the dev repo from a coverage request. */
    public RepoSource resolveDevRepo(AnalysisRequestDto req) {
        if (req.getDevSourceType() == AnalysisRequestDto.SourceType.REMOTE) {
            return remoteService.cloneAndResolve(req.getDevRemoteUrl(), req.getDevBranch());
        }
        return localService.resolve(req.getDevLocalPath());
    }

    /** Resolves the QA repo from a coverage request. */
    public RepoSource resolveQaRepo(AnalysisRequestDto req) {
        if (req.getQaSourceType() == AnalysisRequestDto.SourceType.REMOTE) {
            return remoteService.cloneAndResolve(req.getQaRemoteUrl(), req.getQaBranch());
        }
        return localService.resolve(req.getQaLocalPath());
    }

    /** Resolves the target repo for an understanding request. */
    public RepoSource resolveTargetRepo(AnalysisRequestDto req) {
        if (req.getTargetSourceType() == AnalysisRequestDto.SourceType.REMOTE) {
            return remoteService.cloneAndResolve(req.getTargetRemoteUrl(), req.getTargetBranch());
        }
        return localService.resolve(req.getTargetLocalPath());
    }

    /** Convenience: resolve any source by type, URL/path and branch. */
    public RepoSource resolve(AnalysisRequestDto.SourceType type, String pathOrUrl, String branch) {
        if (type == AnalysisRequestDto.SourceType.REMOTE) {
            return remoteService.cloneAndResolve(pathOrUrl, branch);
        }
        return localService.resolve(pathOrUrl);
    }
}
