package com.anoncircles.graph.context;

/**
 * Per-request GraphQL context. Built by {@code AuthContextInterceptor} from the
 * incoming HTTP request and made available to every resolver via
 * {@code DataFetchingEnvironment.getGraphQlContext()}.
 *
 * <p>Per-request DataLoaders are managed by Spring's {@code BatchLoaderRegistry}
 * (see {@code CircleDataLoaderConfig}); they read this context to obtain the
 * caller's JWT for downstream calls.
 *
 * @param engageAuth the user's JWT (forwarded as-is to discussions-service); null for unauthenticated requests
 */
public record GraphContext(String engageAuth) {
    public static final String KEY = "graphContext";
}
