package com.anoncircles.discussions.lib;

import com.anoncircles.discussions.config.EngageAuthProperties;
import com.anoncircles.discussions.exception.InvalidTokenException;
import com.anoncircles.discussions.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Signs and verifies the {@code engage-auth} JWT.
 *
 * <p>Algorithm: HS256. Subject = user id. Expiry from
 * {@link EngageAuthProperties#expiration()} (default 7 days).
 *
 * <p>Verification failures throw typed exceptions that
 * {@code GlobalExceptionHandler} maps to HTTP 401 with a generic body.
 */
@Slf4j
@Component
public class EngageAuthTokenService {

    private final SecretKey key;
    private final long expirationMillis;

    public EngageAuthTokenService(EngageAuthProperties props) {
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "engage.auth.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMillis = props.expiration().toMillis();
    }

    public String sign(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key)
                .compact();
    }

    /**
     * @throws TokenExpiredException if the JWT's exp claim is in the past
     * @throws InvalidTokenException for every other validation failure (bad
     *         signature, malformed payload, missing subject, etc.)
     */
    public Claims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new InvalidTokenException("Token missing subject");
            }
            return claims;
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("engage-auth token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid engage-auth token: {}", ex.getMessage());
            throw new InvalidTokenException("Invalid engage-auth token");
        }
    }

    public UUID extractUserId(Claims claims) {
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Subject claim is not a valid UUID");
        }
    }
}
