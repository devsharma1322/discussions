package com.anoncircles.graph.unit;

import com.anoncircles.graph.client.RestErrorMapper;
import graphql.GraphqlErrorException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

class RestErrorMapperTest {

    @Test
    void maps401_toUnauthenticated_withGenericMessage() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.UNAUTHORIZED, "real reason"));
        assertThat(ex.getExtensions()).containsEntry("code", "UNAUTHENTICATED");
        assertThat(ex.getMessage()).isEqualTo("Authentication required");
    }

    @Test
    void maps403_toForbidden_withGenericMessage() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.FORBIDDEN, "no admin"));
        assertThat(ex.getExtensions()).containsEntry("code", "FORBIDDEN");
        assertThat(ex.getMessage()).isEqualTo("Forbidden");
    }

    @Test
    void maps404_toNotFound_passesBody() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.NOT_FOUND, "circle missing"));
        assertThat(ex.getExtensions()).containsEntry("code", "NOT_FOUND");
        assertThat(ex.getMessage()).contains("circle missing");
    }

    @Test
    void maps409_toConflict() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.CONFLICT, "already member"));
        assertThat(ex.getExtensions()).containsEntry("code", "CONFLICT");
    }

    @Test
    void maps400_toBadUserInput() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.BAD_REQUEST, "bad json"));
        assertThat(ex.getExtensions()).containsEntry("code", "BAD_USER_INPUT");
    }

    @Test
    void maps429_toRateLimited_withGenericMessage() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.TOO_MANY_REQUESTS, "slow"));
        assertThat(ex.getExtensions()).containsEntry("code", "RATE_LIMITED");
        assertThat(ex.getMessage()).isEqualTo("Too many requests");
    }

    @Test
    void maps500_toInternalServerError() {
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.INTERNAL_SERVER_ERROR, "boom"));
        assertThat(ex.getExtensions()).containsEntry("code", "INTERNAL_SERVER_ERROR");
    }

    @Test
    void capsLargeBodiesAt500Chars() {
        String big = "x".repeat(1000);
        GraphqlErrorException ex = RestErrorMapper.map(buildRestError(HttpStatus.NOT_FOUND, big));
        assertThat(ex.getMessage().length()).isLessThanOrEqualTo(500);
    }

    private static WebClientResponseException buildRestError(HttpStatus status, String body) {
        return WebClientResponseException.create(
                status.value(),
                status.getReasonPhrase(),
                org.springframework.http.HttpHeaders.EMPTY,
                body.getBytes(),
                null);
    }
}
