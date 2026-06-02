package pl.mateuszj.rekrutacja;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
class GithubService {

    private final GithubClient githubClient;

    GithubService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    List<RepositoryResponse> getUserRepositories(String username) {
        List<GithubRepoDto> repos = githubClient.getUserRepositories(username);

        return repos.stream()
                .filter(repo -> !repo.fork())
                .map(repo -> {
                    List<GithubBranchDto> branches = githubClient.getRepositoryBranches(repo.owner().login(), repo.name());
                    List<BranchResponse> branchResponses = branches.stream()
                            .map(branch -> new BranchResponse(branch.name(), branch.commit().sha()))
                            .toList();
                    return new RepositoryResponse(repo.name(), repo.owner().login(), branchResponses);
                })
                .toList();
    }
}
