package com.anoncircles.graph.unit;

import com.anoncircles.graph.client.GenaiClient;
import com.anoncircles.graph.client.GenaiUnavailableException;
import com.anoncircles.graph.config.GraphProperties;
import com.anoncircles.graph.model.GenerateMode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GenaiClientTest {

    private MockWebServer server;
    private GenaiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        GraphProperties props = new GraphProperties(
                "http://unused",
                server.url("/").toString().replaceAll("/$", ""),
                "internal-secret-token",
                "http://localhost:5173",
                new GraphProperties.Query(7));
        client = new GenaiClient(WebClient.builder(), props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void streamsChunks_andSendsInternalAuthHeader() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data:hello \n\ndata:world\n\nevent:done\ndata:\n\n"));

        StepVerifier.create(client.generate("topic", null, GenerateMode.FROM_TOPIC))
                .expectNext("hello ")
                .expectNext("world")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("X-Internal-Auth")).isEqualTo("internal-secret-token");
        assertThat(req.getPath()).isEqualTo("/generate");
        String body = req.getBody().readUtf8();
        assertThat(body).contains("\"topic\":\"topic\"");
        assertThat(body).contains("\"mode\":\"FROM_TOPIC\"");
        assertThat(body).doesNotContain("\"description\":");
    }

    @Test
    void includesDescription_whenProvided() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data:ok\n\nevent:done\ndata:\n\n"));

        client.generate("x", "polish me", GenerateMode.FROM_TOPIC_AND_DESCRIPTION).blockLast();
        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertThat(body).contains("\"description\":\"polish me\"");
        assertThat(body).contains("\"mode\":\"FROM_TOPIC_AND_DESCRIPTION\"");
    }

    @Test
    void unavailableMarker_terminatesWithTypedException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data:{\"status\":\"UNAVAILABLE\",\"message\":\"out of tokens\"}\n\n" +
                        "event:done\ndata:\n\n"));

        StepVerifier.create(client.generate("x", null, GenerateMode.FROM_TOPIC))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(GenaiUnavailableException.class);
                    assertThat(err.getMessage()).isEqualTo("out of tokens");
                })
                .verify();
    }

    @Test
    void httpError_isWrappedAsGenaiUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        StepVerifier.create(client.generate("x", null, GenerateMode.FROM_TOPIC))
                .expectError(GenaiUnavailableException.class)
                .verify();
    }
}
