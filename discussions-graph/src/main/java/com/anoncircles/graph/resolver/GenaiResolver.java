package com.anoncircles.graph.resolver;

import com.anoncircles.graph.client.GenaiClient;
import com.anoncircles.graph.client.GenaiUnavailableException;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.model.DescriptionGenerated;
import com.anoncircles.graph.model.DescriptionUnavailable;
import com.anoncircles.graph.model.GenerateDescriptionResult;
import com.anoncircles.graph.model.GenerateMode;
import com.anoncircles.graph.security.GenerateRateLimiter;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resolver for the {@code generateDescription} mutation.
 *
 * <p>Returns a {@code GenerateDescriptionResult} union — either
 * {@link DescriptionGenerated} or {@link DescriptionUnavailable}. Modelling
 * failure as a typed branch (rather than throwing) forces the React UI to
 * handle the unavailable case at compile time.
 *
 * <p>Per-user rate-limited to 10 calls/hour via {@link GenerateRateLimiter}.
 * Tokens stream from {@link GenaiClient}; we collect them server-side and
 * return the final text in a single response. (Subscription-based incremental
 * delivery is a follow-up ticket; the SSE infrastructure on genai-service is
 * already in place.)
 */
@Controller
@RequiredArgsConstructor
public class GenaiResolver {

    private final GenaiClient genaiClient;
    private final GenerateRateLimiter rateLimiter;

    @MutationMapping
    public Mono<GenerateDescriptionResult> generateDescription(
            @Argument String topic,
            @Argument String description,
            @Argument GenerateMode mode,
            DataFetchingEnvironment env) {

        GraphContext ctx = requireContext(env);
        if (!rateLimiter.tryAcquire(ctx)) {
            return Mono.error(GraphqlErrorException.newErrorException()
                    .message("Too many AI generations — try again in an hour.")
                    .extensions(Map.of("code", "RATE_LIMITED"))
                    .build());
        }

        return genaiClient.generate(topic, description, mode)
                .collectList()
                .map(chunks -> (GenerateDescriptionResult) new DescriptionGenerated(
                        String.join("", chunks)))
                .onErrorResume(GenaiUnavailableException.class,
                        ex -> Mono.just(new DescriptionUnavailable(ex.getMessage())));
    }

    /**
     * Streams the generated description token-by-token. The UI subscribes over
     * WebSocket and appends each chunk to a textarea as it arrives, so users
     * see progressive fill instead of a 3-second freeze followed by the full
     * response. Same rate-limit bucket as the mutation, same auth.
     *
     * <p>On upstream failure the Flux completes with an error; Spring for
     * GraphQL emits a single {@code error} event over the WS subscription and
     * closes the stream. The UI maps that to the same "unavailable" UX as the
     * mutation's {@code DescriptionUnavailable} union branch.
     */
    @SubscriptionMapping
    public Flux<String> generateDescriptionStream(
            @Argument String topic,
            @Argument String description,
            @Argument GenerateMode mode,
            DataFetchingEnvironment env) {

        GraphContext ctx = requireContext(env);
        if (!rateLimiter.tryAcquire(ctx)) {
            return Flux.error(GraphqlErrorException.newErrorException()
                    .message("Too many AI generations — try again in an hour.")
                    .extensions(Map.of("code", "RATE_LIMITED"))
                    .build());
        }

        // Gemini chunks tend to arrive in 2–3 large bursts (sometimes the
        // entire response in one shot for short prompts), which defeats the
        // "typing" UX. We re-chunk every upstream emission into word-sized
        // pieces and emit them with a small inter-word delay so the textarea
        // fills progressively, simulating the perceived feel of a real LLM
        // streaming response. The total wall-clock time is unchanged — we're
        // just smoothing the burst.
        return genaiClient.generate(topic, description, mode)
                .concatMap(chunk -> Flux
                        .fromArray(chunk.split("(?<=\\s)"))
                        .filter(s -> !s.isEmpty())
                        .delayElements(java.time.Duration.ofMillis(40)))
                .onErrorMap(GenaiUnavailableException.class,
                        ex -> GraphqlErrorException.newErrorException()
                                .message(ex.getMessage())
                                .extensions(Map.of("code", "UNAVAILABLE"))
                                .build());
    }

    private static GraphContext requireContext(DataFetchingEnvironment env) {
        GraphContext ctx = env.getGraphQlContext().get(GraphContext.KEY);
        if (ctx == null || ctx.engageAuth() == null || ctx.engageAuth().isBlank()) {
            throw GraphqlErrorException.newErrorException()
                    .message("Authentication required")
                    .extensions(Map.of("code", "UNAUTHENTICATED"))
                    .build();
        }
        return ctx;
    }
}
