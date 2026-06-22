package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.exception.GlobalExceptionHandler;
import com.anoncircles.discussions.exception.InvalidTokenException;
import com.anoncircles.discussions.exception.RateLimitExceededException;
import com.anoncircles.discussions.exception.TokenExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDenied_mapsTo403() {
        ResponseEntity<Map<String, Object>> r = handler.handleForbidden(new AccessDeniedException("no"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).containsEntry("status", 403);
    }

    @Test
    void invalidToken_mapsTo401() {
        ResponseEntity<Map<String, Object>> r = handler.handleAuthFailure(new InvalidTokenException("bad"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredToken_mapsTo401() {
        ResponseEntity<Map<String, Object>> r = handler.handleAuthFailure(new TokenExpiredException("exp"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rateLimit_mapsTo429_withRetryAfterHeader() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleRateLimit(new RateLimitExceededException("slow down", Duration.ofMinutes(1)));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(r.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(r.getBody()).containsEntry("message", "slow down");
    }

    @Test
    void responseStatus_passesThrough() {
        ResponseEntity<Map<String, Object>> r = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "missing"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("message", "missing");
    }

    @Test
    void unknown_mapsTo500_withSanitizedBody() {
        ResponseEntity<Map<String, Object>> r = handler.handleUnknown(new RuntimeException("secret stack trace"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // Body must not leak the original message.
        assertThat(r.getBody().get("message").toString()).doesNotContain("secret");
    }
}
