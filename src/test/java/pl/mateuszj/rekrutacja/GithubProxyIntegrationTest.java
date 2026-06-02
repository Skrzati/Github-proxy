package pl.mateuszj.rekrutacja;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WireMockTest(httpPort = 9999)
class GithubProxyIntegrationTest {
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("github.api.url", () -> "http://localhost:9999");
    }
    @Test
    void shouldReturnNonForkRepositoriesWithBranchesForExistingUser() throws Exception {
        // Stub for User Repositories
        stubFor(get(urlEqualTo("/users/test-user/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "name": "repo-1",
                                    "fork": false,
                                    "owner": { "login": "test-user" }
                                  },
                                  {
                                    "name": "repo-2",
                                    "fork": true,
                                    "owner": { "login": "test-user" }
                                  }
                                ]
                                """)));
        // Stub for branches of repo-1 (non-fork)
        stubFor(get(urlEqualTo("/repos/test-user/repo-1/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "name": "main",
                                    "commit": { "sha": "abcdef123456" }
                                  }
                                ]
                                """)));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/repositories/test-user")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].repositoryName", is("repo-1")))
                .andExpect(jsonPath("$[0].ownerLogin", is("test-user")))
                .andExpect(jsonPath("$[0].branches", hasSize(1)))
                .andExpect(jsonPath("$[0].branches[0].name", is("main")))
                .andExpect(jsonPath("$[0].branches[0].lastCommitSha", is("abcdef123456")));
    }
    @Test
    void shouldReturn404ErrorJsonWhenUserDoesNotExistOnGithub() throws Exception {
        stubFor(get(urlEqualTo("/users/non-existing-user/repos"))
                .willReturn(aResponse()
                        .withStatus(404)));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/repositories/non-existing-user")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("User not found on GitHub: non-existing-user")));
    }
    @Test
    void shouldReturn406NotAcceptableWhenUnsupportedMediaTypeIsRequested() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/repositories/test-user")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE))
                .andExpect(status().isNotAcceptable());
    }
}