package com.anoncircles.graph.resolver;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.model.AuthResult;
import com.anoncircles.graph.model.GenericResult;
import com.anoncircles.graph.model.User;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * GraphQL resolvers for the anonymous session lifecycle.
 *
 * <p>Replaces the previous email/password resolvers. All side-effecting
 * mutations are reactive ({@link Mono}) and delegate to
 * {@link DiscussionsClient} which handles {@code Authorization} forwarding
 * and REST → typed-GraphQL-error mapping.
 */
@Controller
@RequiredArgsConstructor
public class AuthResolver {

    private final DiscussionsClient client;

    /**
     * Public — every visitor calls this (no token required). Returns a fresh
     * anonymous {@link AuthResult}.
     */
    @MutationMapping
    public Mono<AuthResult> startSession() {
        return client.startSession();
    }

    /**
     * Forwarded to {@code discussions-service /auth/logout} (no-op today). The
     * UI is responsible for clearing its local token and resetting the Apollo
     * cache. See {@code useAuth.startNewIdentity()}.
     */
    @MutationMapping
    public Mono<GenericResult> logout(DataFetchingEnvironment env) {
        String engageAuth = requireToken(env);
        return client.logout(engageAuth);
    }

    /**
     * Returns the authenticated user. Unauthenticated callers see a typed
     * {@code UNAUTHENTICATED} GraphQL error.
     */
    @QueryMapping
    public Mono<User> me(DataFetchingEnvironment env) {
        String engageAuth = requireToken(env);
        return client.me(engageAuth);
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
