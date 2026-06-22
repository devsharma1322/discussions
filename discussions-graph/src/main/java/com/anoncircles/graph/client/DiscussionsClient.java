package com.anoncircles.graph.client;

import com.anoncircles.graph.config.GraphProperties;
import com.anoncircles.graph.model.AuthResult;
import com.anoncircles.graph.model.Circle;
import com.anoncircles.graph.model.CirclePage;
import com.anoncircles.graph.model.CircleScope;
import com.anoncircles.graph.model.CircleSort;
import com.anoncircles.graph.model.GenericResult;
import com.anoncircles.graph.model.Message;
import com.anoncircles.graph.model.MessagePage;
import com.anoncircles.graph.model.Thread;
import com.anoncircles.graph.model.ThreadPage;
import com.anoncircles.graph.model.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed REST wrapper around {@code discussions-service}.
 *
 * <p>Every method that requires authentication accepts an {@code engageAuth}
 * argument; a {@link ExchangeFilterFunction} adds it as
 * {@code Authorization: Bearer <token>} so resolvers can't accidentally forget.
 *
 * <p>4xx/5xx responses are converted via {@link RestErrorMapper} so resolvers
 * see {@code Mono.error(GraphqlErrorException)} with the right
 * {@code extensions.code} (e.g. UNAUTHENTICATED, FORBIDDEN, CONFLICT).
 */
@Component
public class DiscussionsClient {

    private final WebClient http;

    public DiscussionsClient(WebClient.Builder builder, GraphProperties props) {
        this.http = builder
                .baseUrl(props.discussionsServiceUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ===== Session =====

    public Mono<AuthResult> startSession() {
        return http.post().uri("/auth/session")
                .retrieve()
                .bodyToMono(AuthResult.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<GenericResult> logout(String engageAuth) {
        return http.post().uri("/auth/logout")
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .toBodilessEntity()
                .map(r -> GenericResult.ok())
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<User> me(String engageAuth) {
        return http.get().uri("/auth/me")
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(User.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    // ===== Circles =====

    public Mono<CirclePage> getCircles(
            String engageAuth, CircleScope scope, CircleSort sort,
            String search, int page, int limit) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("scope", scope.restValue());
        params.add("sort", sort.restValue());
        if (search != null && !search.isBlank()) {
            params.add("search", search);
        }
        params.add("page", String.valueOf(page));
        params.add("limit", String.valueOf(limit));

        return http.get().uri(uri -> uri.path("/circles").queryParams(params).build())
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(CirclePage.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Circle> getCircle(String engageAuth, UUID id) {
        return http.get().uri("/circles/{id}", id)
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(Circle.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Circle> createCircle(String engageAuth, String topic, String description) {
        return http.post().uri("/circles")
                .headers(h -> withBearer(h, engageAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("topic", topic, "description", description))
                .retrieve()
                .bodyToMono(Circle.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Circle> updateCircleDescription(String engageAuth, UUID id, String description) {
        return http.patch().uri("/circles/{id}", id)
                .headers(h -> withBearer(h, engageAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("description", description))
                .retrieve()
                .bodyToMono(Circle.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<GenericResult> deleteCircle(String engageAuth, UUID id) {
        return http.delete().uri("/circles/{id}", id)
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .toBodilessEntity()
                .map(r -> GenericResult.ok())
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Circle> joinCircle(String engageAuth, UUID id) {
        // discussions-service returns {"handle":"..."}; resolver re-fetches the Circle
        // separately so it can return the now-isMember=true row to Apollo.
        return http.post().uri("/circles/{id}/join", id)
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .toBodilessEntity()
                .then(getCircle(engageAuth, id))
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Circle> leaveCircle(String engageAuth, UUID id) {
        return http.delete().uri("/circles/{id}/join", id)
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .toBodilessEntity()
                .then(getCircle(engageAuth, id))
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    // ===== Threads =====

    public Mono<ThreadPage> getThreads(String engageAuth, UUID circleId, int page, int limit) {
        return http.get().uri(uri -> uri.path("/circles/{id}/threads")
                        .queryParam("page", page).queryParam("limit", limit).build(circleId))
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(ThreadPage.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Thread> getThread(String engageAuth, UUID id) {
        return http.get().uri("/threads/{id}", id)
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(Thread.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Thread> createThread(String engageAuth, UUID circleId, String title) {
        return http.post().uri("/circles/{id}/threads", circleId)
                .headers(h -> withBearer(h, engageAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", title))
                .retrieve()
                .bodyToMono(Thread.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    // ===== Messages =====

    public Mono<MessagePage> getMessages(String engageAuth, UUID threadId, int page, int limit) {
        return http.get().uri(uri -> uri.path("/threads/{id}/messages")
                        .queryParam("page", page).queryParam("limit", limit).build(threadId))
                .headers(h -> withBearer(h, engageAuth))
                .retrieve()
                .bodyToMono(MessagePage.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    public Mono<Message> postMessage(String engageAuth, UUID threadId, String body) {
        return http.post().uri("/threads/{id}/messages", threadId)
                .headers(h -> withBearer(h, engageAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("body", body))
                .retrieve()
                .bodyToMono(Message.class)
                .onErrorMap(WebClientResponseException.class, RestErrorMapper::map);
    }

    // ===== helpers =====

    private static void withBearer(HttpHeaders headers, String engageAuth) {
        Objects.requireNonNull(engageAuth, "engageAuth required");
        headers.setBearerAuth(engageAuth);
    }

    /** Reified type ref kept around in case resolvers need raw JSON shapes later. */
    @SuppressWarnings("unused")
    private static final ParameterizedTypeReference<Map<String, Object>> RAW_MAP =
            new ParameterizedTypeReference<>() {};
}
