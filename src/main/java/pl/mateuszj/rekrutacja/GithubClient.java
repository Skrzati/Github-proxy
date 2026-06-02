package pl.mateuszj.rekrutacja;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component
class GithubClient {

    private final RestClient restClient;

    GithubClient(RestClient.Builder restClientBuilder, @Value("${github.api.url:https://api.github.com}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "github-proxy-app")
                .build();
    }

    List<GithubRepoDto> getUserRepositories(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}/repos", username)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new UserNotFoundException("User not found on GitHub: " + username);
                        }
                        throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText());
                    })
                    .body(new ParameterizedTypeReference<List<GithubRepoDto>>() {});
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException("User not found on GitHub: " + username);
        }
    }

    List<GithubBranchDto> getRepositoryBranches(String owner, String repo) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GithubBranchDto>>() {});
    }
}
