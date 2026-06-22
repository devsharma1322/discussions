package com.anoncircles.graph.config;

import graphql.GraphqlErrorException;
import graphql.GraphQLError;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import graphql.schema.DataFetchingEnvironment;

/**
 * Bridges our typed {@link GraphqlErrorException}s (thrown by resolvers and
 * downstream clients) to the GraphQL response with {@code extensions.code}
 * preserved.
 *
 * <p>Without this, Spring for GraphQL's default sanitiser maps every
 * resolver-thrown exception to {@code INTERNAL_ERROR} with a generated UUID —
 * which hides our intentional {@code UNAUTHENTICATED}, {@code FORBIDDEN},
 * {@code RATE_LIMITED} codes from clients.
 */
@Component
public class TypedErrorResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof GraphqlErrorException gex) {
            return gex;
        }
        return null; // fall through to other resolvers / default handling
    }
}
