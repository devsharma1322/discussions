package com.anoncircles.graph.resolver;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.loader.CircleDataLoaderConfig;
import com.anoncircles.graph.model.Circle;
import com.anoncircles.graph.model.CirclePage;
import com.anoncircles.graph.model.CircleScope;
import com.anoncircles.graph.model.CircleSort;
import com.anoncircles.graph.model.GenericResult;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL resolvers for the circles surface — listing, fetching, CRUD,
 * join/leave. Delegates to {@link DiscussionsClient}; all error mapping
 * (4xx → typed GraphQL errors) is handled there.
 */
@Controller
@RequiredArgsConstructor
public class CircleResolver {

    /** UI-visible hard cap. Mirrors the server-side {@code PageResponse.MAX_LIMIT}. */
    public static final int MAX_LIMIT = 10;

    private final DiscussionsClient client;

    // ===== Queries =====

    @QueryMapping
    public Mono<CirclePage> circles(
            @Argument CircleScope scope,
            @Argument CircleSort sort,
            @Argument String search,
            @Argument Integer page,
            @Argument Integer limit,
            DataFetchingEnvironment env) {
        String token = requireToken(env);
        int p = page == null || page < 1 ? 1 : page;
        int l = clampLimit(limit);
        return client.getCircles(token,
                scope == null ? CircleScope.ALL : scope,
                sort == null ? CircleSort.POPULAR : sort,
                search,
                p, l);
    }

    @QueryMapping
    public CompletableFuture<Circle> circle(@Argument UUID id,
                                            DataLoader<UUID, Circle> circleById,
                                            DataFetchingEnvironment env) {
        requireToken(env);
        return circleById.load(id);
    }

    // ===== Mutations =====

    @MutationMapping
    public Mono<Circle> createCircle(@Argument String topic,
                                     @Argument String description,
                                     DataFetchingEnvironment env) {
        return client.createCircle(requireToken(env), topic, description);
    }

    @MutationMapping
    public Mono<Circle> updateCircleDescription(@Argument UUID id,
                                                @Argument String description,
                                                DataFetchingEnvironment env) {
        return client.updateCircleDescription(requireToken(env), id, description);
    }

    @MutationMapping
    public Mono<GenericResult> deleteCircle(@Argument UUID id, DataFetchingEnvironment env) {
        return client.deleteCircle(requireToken(env), id);
    }

    @MutationMapping
    public Mono<Circle> joinCircle(@Argument UUID id, DataFetchingEnvironment env) {
        return client.joinCircle(requireToken(env), id);
    }

    @MutationMapping
    public Mono<Circle> leaveCircle(@Argument UUID id, DataFetchingEnvironment env) {
        return client.leaveCircle(requireToken(env), id);
    }

    // ===== helpers =====

    static int clampLimit(Integer requested) {
        if (requested == null) return MAX_LIMIT;
        return Math.max(1, Math.min(MAX_LIMIT, requested));
    }

    private static String requireToken(DataFetchingEnvironment env) {
        GraphContext ctx = env.getGraphQlContext().get(GraphContext.KEY);
        if (ctx == null || ctx.engageAuth() == null || ctx.engageAuth().isBlank()) {
            throw GraphqlErrorException.newErrorException()
                    .message("Authentication required")
                    .extensions(Map.of("code", "UNAUTHENTICATED"))
                    .build();
        }
        return ctx.engageAuth();
    }
}
