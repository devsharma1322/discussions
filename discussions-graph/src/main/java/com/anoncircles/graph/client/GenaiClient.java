package com.anoncircles.graph.client;

import com.anoncircles.graph.config.GraphProperties;
import com.anoncircles.graph.model.GenerateMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * Reactive client for {@code genai-service}. Sends the {@code X-Internal-Auth}
 * shared secret on every call (never logged, never exposed to the UI) and
 * consumes the SSE stream returned by {@code POST /generate}.
 *
 * <p>If any event signals {@code UNAVAILABLE} or the stream errors, the
 * returned {@link Flux} terminates with {@link GenaiUnavailableException}.
 * The resolver maps that to the typed {@code DescriptionUnavailable} union
 * branch.
 */
@Component
public class GenaiClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_REF =
            new ParameterizedTypeReference<>() {};

    private final WebClient http;
    private final String internalAuthToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenaiClient(WebClient.Builder builder, GraphProperties props) {
        this.http = builder.baseUrl(props.genaiServiceUrl()).build();
        this.internalAuthToken = props.internalAuthToken();
    }

    public Flux<String> generate(String topic, String description, GenerateMode mode) {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic);
        body.put("mode", mode.name());
        if (description != null && !description.isBlank()) {
            body.put("description", description);
        }

        return http.post().uri("/generate")
                .header("X-Internal-Auth", internalAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(SSE_STRING_REF)
                .concatMap(this::processEvent)
                .onErrorMap(ex -> !(ex instanceof GenaiUnavailableException),
                        ex -> new GenaiUnavailableException("genai-service unreachable", ex));
    }

    /** Decodes an SSE chunk; signals {@link GenaiUnavailableException} on UNAVAILABLE. */
    private Flux<String> processEvent(ServerSentEvent<String> event) {
        String data = event.data();
        if (data == null || data.isEmpty()) {
            return Flux.empty();
        }
        // Quick prefix check before paying the JSON parse cost.
        if (data.contains("\"status\"") && data.contains("UNAVAILABLE")) {
            try {
                JsonNode node = mapper.readTree(data);
                if (node.has("status") && "UNAVAILABLE".equals(node.get("status").asText())) {
                    String message = node.has("message") ? node.get("message").asText() : "AI is unavailable";
                    return Flux.error(new GenaiUnavailableException(message));
                }
            } catch (Exception ignored) {
                // Not valid JSON — treat as a regular text chunk.
            }
        }
        return Flux.just(data);
    }
}
