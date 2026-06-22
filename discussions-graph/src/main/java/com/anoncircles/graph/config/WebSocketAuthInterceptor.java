package com.anoncircles.graph.config;

import com.anoncircles.graph.context.GraphContext;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Per-connection auth for GraphQL subscriptions delivered over WebSocket
 * (graphql-ws subprotocol).
 *
 * <p>Browsers can't attach custom HTTP headers to {@code new WebSocket(...)},
 * so the standard pattern is to send {@code {"Authorization":"Bearer <jwt>"}}
 * inside the {@code connection_init} payload. This interceptor pulls the
 * token out of that payload, stashes it on the WS session, and then on every
 * subscription operation re-binds it onto the per-request {@link GraphContext}
 * so resolvers see the same auth signal whether the call arrived over HTTP or
 * WS.
 *
 * <p>If the {@code connection_init} payload omits {@code Authorization}, the
 * connection is still accepted — resolvers that require auth will reject the
 * subscription with {@code UNAUTHENTICATED}. We deliberately do not slam the
 * door at connection-init time so that anonymous queries (e.g. introspection
 * in dev) can still subscribe.
 */
@Component
public class WebSocketAuthInterceptor implements WebSocketGraphQlInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_TOKEN_KEY = "engageAuth";

    @Override
    public Mono<Object> handleConnectionInitialization(WebSocketSessionInfo sessionInfo,
                                                       Map<String, Object> connectionInitPayload) {
        Object headerValue = connectionInitPayload == null ? null : connectionInitPayload.get(AUTHORIZATION);
        if (headerValue instanceof String s && s.startsWith(BEARER_PREFIX)) {
            sessionInfo.getAttributes().put(SESSION_TOKEN_KEY, s.substring(BEARER_PREFIX.length()).trim());
        }
        return Mono.empty();
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        // Only act on requests that carry a WebSocketSessionInfo (i.e. WS subscriptions).
        if (request instanceof org.springframework.graphql.server.WebSocketGraphQlRequest wsReq) {
            String token = (String) wsReq.getSessionInfo().getAttributes().get(SESSION_TOKEN_KEY);
            if (token != null && !token.isBlank()) {
                GraphContext ctx = new GraphContext(token);
                request.configureExecutionInput((input, builder) ->
                        builder.graphQLContext(c -> c.put(GraphContext.KEY, ctx)).build());
            }
        }
        return chain.next(request);
    }
}
