package com.anoncircles.graph.client;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Maps REST 4xx/5xx responses from {@code discussions-service} into typed
 * GraphQL errors with a stable {@code extensions.code} so the React UI and
 * downstream observability tools have a contract to key on.
 *
 * <p>Never echoes downstream stack traces back to the caller — only the body
 * message (sanitised by the upstream service) propagates.
 */
public final class RestErrorMapper {

    private RestErrorMapper() {}

    public static GraphqlErrorException map(WebClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String code = switch (status) {
            case 400 -> "BAD_USER_INPUT";
            case 401 -> "UNAUTHENTICATED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 429 -> "RATE_LIMITED";
            default  -> status >= 500 ? "INTERNAL_SERVER_ERROR" : "BAD_REQUEST";
        };
        ErrorClassification classification = switch (code) {
            case "BAD_USER_INPUT", "BAD_REQUEST" -> ErrorType.ValidationError;
            case "UNAUTHENTICATED", "FORBIDDEN"  -> ErrorType.DataFetchingException;
            default                              -> ErrorType.DataFetchingException;
        };

        return GraphqlErrorException.newErrorException()
                .message(messageFor(code, ex))
                .errorClassification(classification)
                .extensions(Map.of("code", code, "downstreamStatus", status))
                .build();
    }

    private static String messageFor(String code, WebClientResponseException ex) {
        // Generic messages for security-sensitive codes; pass-through otherwise.
        return switch (code) {
            case "UNAUTHENTICATED" -> "Authentication required";
            case "FORBIDDEN"       -> "Forbidden";
            case "RATE_LIMITED"    -> "Too many requests";
            default                -> safeBody(ex);
        };
    }

    private static String safeBody(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getStatusCode().toString();
        }
        return body.length() > 500 ? body.substring(0, 500) : body;
    }

    /** Convenience for non-WebClient errors that already implement {@link GraphQLError}. */
    public static GraphQLError asGraphQLError(GraphqlErrorException ex) {
        return ex;
    }
}
