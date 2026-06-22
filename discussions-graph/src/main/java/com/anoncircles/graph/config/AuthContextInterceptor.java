package com.anoncircles.graph.config;

import com.anoncircles.graph.context.GraphContext;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Extracts {@code Authorization: Bearer <token>} from the incoming GraphQL HTTP
 * request and stashes it on the {@link org.springframework.graphql.execution.GraphQlContext}.
 * Resolvers and per-request DataLoaders read it from there.
 *
 * <p>Subscription requests delivered over WebSocket are handled separately by
 * {@link WebSocketAuthInterceptor} (browsers can't set custom WS upgrade
 * headers, so the token rides inside the {@code connection_init} payload).
 * This interceptor skips WS requests to avoid clobbering the context set by
 * the WS interceptor with a {@code null} token.
 */
@Component
@RequiredArgsConstructor
public class AuthContextInterceptor implements WebGraphQlInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (request instanceof WebSocketGraphQlRequest) {
            return chain.next(request);
        }
        String engageAuth = extractBearer(request.getHeaders());
        GraphContext ctx = new GraphContext(engageAuth);
        request.configureExecutionInput((input, builder) ->
                builder.graphQLContext(c -> c.put(GraphContext.KEY, ctx)).build());
        return chain.next(request);
    }

    private static String extractBearer(HttpHeaders headers) {
        String authz = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authz == null || !authz.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authz.substring(BEARER_PREFIX.length()).trim();
    }
}

