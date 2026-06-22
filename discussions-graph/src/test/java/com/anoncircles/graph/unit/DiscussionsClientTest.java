package com.anoncircles.graph.unit;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.config.GraphProperties;
import com.anoncircles.graph.model.AuthResult;
import com.anoncircles.graph.model.Circle;
import com.anoncircles.graph.model.CirclePage;
import com.anoncircles.graph.model.CircleScope;
import com.anoncircles.graph.model.CircleSort;
import com.anoncircles.graph.model.User;
import graphql.GraphqlErrorException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DiscussionsClientTest {

    private MockWebServer server;
    private DiscussionsClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        GraphProperties props = new GraphProperties(
                server.url("/").toString().replaceAll("/$", ""),
                "http://unused",
                "internal-token-not-used-here",
                "http://localhost:5173",
                new GraphProperties.Query(7));
        client = new DiscussionsClient(WebClient.builder(), props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void startSession_postsAndDecodes() throws InterruptedException {
        UUID uid = UUID.randomUUID();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"engageAuth\":\"jwt.value\"," +
                        "\"user\":{\"id\":\"" + uid + "\",\"displayName\":\"BoldFox-12\"," +
                        "\"createdAt\":\"2024-01-01T00:00:00Z\"}}"));

        AuthResult result = client.startSession().block();

        assertThat(result).isNotNull();
        assertThat(result.engageAuth()).isEqualTo("jwt.value");
        assertThat(result.user().displayName()).isEqualTo("BoldFox-12");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/auth/session");
    }

    @Test
    void me_forwardsBearerHeader() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"" + UUID.randomUUID() + "\",\"displayName\":\"X-01\"," +
                        "\"createdAt\":\"2024-01-01T00:00:00Z\"}"));

        User u = client.me("the-token").block();
        assertThat(u).isNotNull();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer the-token");
    }

    @Test
    void getCircles_encodesAllQueryParams() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[],\"total\":0,\"page\":1,\"limit\":10}"));

        CirclePage p = client.getCircles(
                "tok", CircleScope.DISCOVER, CircleSort.NEWEST, "kotlin", 2, 10).block();
        assertThat(p).isNotNull();
        assertThat(p.total()).isEqualTo(0);

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath())
                .startsWith("/circles?")
                .contains("scope=discover")
                .contains("sort=newest")
                .contains("search=kotlin")
                .contains("page=2")
                .contains("limit=10");
    }

    @Test
    void getCircles_omitsBlankSearch() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[],\"total\":0,\"page\":1,\"limit\":10}"));

        client.getCircles("tok", CircleScope.ALL, CircleSort.POPULAR, "  ", 1, 10).block();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).doesNotContain("search=");
    }

    @Test
    void createCircle_sendsBody() throws InterruptedException {
        UUID id = UUID.randomUUID();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"" + id + "\",\"topic\":\"rust\",\"description\":\"safety\"," +
                        "\"memberCount\":1,\"adminUserId\":\"" + UUID.randomUUID() + "\"," +
                        "\"isAdmin\":true,\"isMember\":true,\"createdAt\":\"2024-01-01T00:00:00Z\"}"));

        Circle c = client.createCircle("tok", "rust", "safety").block();
        assertThat(c).isNotNull();
        assertThat(c.topic()).isEqualTo("rust");

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/circles");
        String body = req.getBody().readUtf8();
        assertThat(body).contains("\"topic\":\"rust\"");
        assertThat(body).contains("\"description\":\"safety\"");
    }

    @Test
    void getCircle_mapsForbiddenToTypedGraphqlError() {
        server.enqueue(new MockResponse().setResponseCode(403).setBody("nope"));

        StepVerifier.create(client.getCircle("tok", UUID.randomUUID()))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(GraphqlErrorException.class);
                    GraphqlErrorException g = (GraphqlErrorException) err;
                    assertThat(g.getExtensions()).containsEntry("code", "FORBIDDEN");
                })
                .verify();
    }

    @Test
    void joinCircle_postsThenFetchesCircle() throws InterruptedException {
        UUID id = UUID.randomUUID();
        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"" + id + "\",\"topic\":\"t\",\"description\":\"d\"," +
                        "\"memberCount\":2,\"adminUserId\":\"" + UUID.randomUUID() + "\"," +
                        "\"isAdmin\":false,\"isMember\":true,\"createdAt\":\"2024-01-01T00:00:00Z\"}"));

        Circle c = client.joinCircle("tok", id).block();
        assertThat(c).isNotNull();
        assertThat(c.isMember()).isTrue();

        RecordedRequest join = server.takeRequest();
        assertThat(join.getMethod()).isEqualTo("POST");
        assertThat(join.getPath()).isEqualTo("/circles/" + id + "/join");
        RecordedRequest get = server.takeRequest();
        assertThat(get.getMethod()).isEqualTo("GET");
        assertThat(get.getPath()).isEqualTo("/circles/" + id);
    }

    @Test
    void deleteCircle_returnsGenericOk() throws InterruptedException {
        UUID id = UUID.randomUUID();
        server.enqueue(new MockResponse().setResponseCode(204));
        var r = client.deleteCircle("tok", id).block();
        assertThat(r).isNotNull();
        assertThat(r.success()).isTrue();
    }
}
