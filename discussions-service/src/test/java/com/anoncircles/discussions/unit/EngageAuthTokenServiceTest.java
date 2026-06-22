package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.config.EngageAuthProperties;
import com.anoncircles.discussions.exception.InvalidTokenException;
import com.anoncircles.discussions.exception.TokenExpiredException;
import com.anoncircles.discussions.lib.EngageAuthTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EngageAuthTokenServiceTest {

    private static final String SECRET = "unit-test-secret-must-be-at-least-32-bytes-long!!";

    private final EngageAuthTokenService service = new EngageAuthTokenService(
            new EngageAuthProperties(SECRET, Duration.ofMinutes(15)));

    @Test
    void signedTokenRoundTrips() {
        UUID userId = UUID.randomUUID();
        String token = service.sign(userId);
        Claims claims = service.verify(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(service.extractUserId(claims)).isEqualTo(userId);
    }

    @Test
    void signedTokenHasExpiryInFuture() {
        Claims claims = service.verify(service.sign(UUID.randomUUID()));
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void verifyRejectsExpiredToken() {
        String expired = expiredToken(UUID.randomUUID());
        assertThatThrownBy(() -> service.verify(expired))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void verifyRejectsGarbage() {
        assertThatThrownBy(() -> service.verify("this-is-not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyRejectsTokenSignedWithDifferentKey() {
        String foreign = foreignToken(UUID.randomUUID());
        assertThatThrownBy(() -> service.verify(foreign))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void extractUserIdRejectsNonUuidSubject() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String bad = Jwts.builder()
                .subject("not-a-uuid")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key)
                .compact();
        Claims claims = service.verify(bad);
        assertThatThrownBy(() -> service.extractUserId(claims))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void constructorRejectsShortSecret() {
        assertThatThrownBy(() ->
                new EngageAuthTokenService(new EngageAuthProperties("too-short", Duration.ofDays(7))))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- helpers ----

    private String expiredToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minus(Duration.ofMinutes(5));
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();
    }

    private String foreignToken(UUID userId) {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-also-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(otherKey)
                .compact();
    }
}
