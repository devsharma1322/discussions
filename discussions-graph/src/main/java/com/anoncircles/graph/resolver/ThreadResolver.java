package com.anoncircles.graph.resolver;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.model.Message;
import com.anoncircles.graph.model.MessagePage;
import com.anoncircles.graph.model.Thread;
import com.anoncircles.graph.model.ThreadPage;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL resolvers for threads and messages.
 *
 * <p>Like the circles resolver, all error mapping (membership 403 → FORBIDDEN,
 * rate limit 429 → RATE_LIMITED, etc.) happens inside the
 * {@link DiscussionsClient}; resolvers stay thin.
 */
@Controller
@RequiredArgsConstructor
public class ThreadResolver {

    /** Mirrors the server-side page-size cap. */
    public static final int MAX_LIMIT = 10;

    private final DiscussionsClient client;

    // ===== Queries =====

    @QueryMapping
    public Mono<Thread> thread(@Argument UUID id, DataFetchingEnvironment env) {
        return client.getThread(requireToken(env), id);
    }

    @QueryMapping
    public Mono<ThreadPage> threads(@Argument UUID circleId,
                                    @Argument Integer page,
                                    @Argument Integer limit,
                                    DataFetchingEnvironment env) {
        return client.getThreads(requireToken(env), circleId, normalizePage(page), clampLimit(limit));
    }

    @QueryMapping
    public Mono<MessagePage> messages(@Argument UUID threadId,
                                      @Argument Integer page,
                                      @Argument Integer limit,
                                      DataFetchingEnvironment env) {
        return client.getMessages(requireToken(env), threadId, normalizePage(page), clampLimit(limit));
    }

    // ===== Mutations =====

    @MutationMapping
    public Mono<Thread> createThread(@Argument UUID circleId,
                                     @Argument String title,
                                     DataFetchingEnvironment env) {
        return client.createThread(requireToken(env), circleId, title);
    }

    @MutationMapping
    public Mono<Message> postMessage(@Argument UUID threadId,
                                     @Argument String body,
                                     DataFetchingEnvironment env) {
        return client.postMessage(requireToken(env), threadId, body);
    }

    // ===== helpers =====

    static int normalizePage(Integer p) {
        return p == null || p < 1 ? 1 : p;
    }

    static int clampLimit(Integer l) {
        if (l == null) return MAX_LIMIT;
        return Math.max(1, Math.min(MAX_LIMIT, l));
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
