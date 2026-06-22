package com.anoncircles.graph.loader;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.model.Circle;
import graphql.GraphQLContext;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;

/**
 * Registers the {@code circleById} mapped batch loader with Spring's
 * {@link BatchLoaderRegistry}.
 *
 * <p>Spring for GraphQL builds a fresh {@code DataLoaderRegistry} from this
 * registry on every request and wires automatic dispatch around each execution
 * cycle, so resolvers can simply declare a {@code DataLoader<UUID, Circle>}
 * parameter (auto-resolved by type) without manual {@code dispatchAll()} calls.
 *
 * <p>The loader pulls the caller's JWT off the per-request {@link GraphContext}
 * (placed there by {@code AuthContextInterceptor}) — so the same global loader
 * works for any authenticated user without leaking tokens across requests.
 *
 * <p>Real-world batching wins: when a single GraphQL operation references
 * multiple circles (e.g. dashboards aliasing {@code circle(id:...)} N times,
 * or future {@code Thread.circle} field resolvers), all ids in a single tick
 * are flattened into one concurrent fan-out instead of N sequential calls.
 */
@Component
@RequiredArgsConstructor
public class CircleDataLoaderConfig {

    public static final String CIRCLE_BY_ID = "circleById";

    private final BatchLoaderRegistry registry;
    private final DiscussionsClient client;

    @PostConstruct
    void register() {
        registry.<UUID, Circle>forName(CIRCLE_BY_ID)
                .registerMappedBatchLoader((ids, env) -> {
                    GraphQLContext gqlCtx = env.getContext();
                    GraphContext ctx = gqlCtx == null ? null : gqlCtx.get(GraphContext.KEY);
                    String token = ctx == null ? null : ctx.engageAuth();
                    if (token == null || token.isBlank()) {
                        return Mono.error(new IllegalStateException(
                                "circleById loader invoked without engageAuth in context"));
                    }
                    return Flux.fromIterable(ids)
                            .flatMap(id -> client.getCircle(token, id)
                                    .map(circle -> Map.entry(id, circle)))
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue);
                });
    }
}
