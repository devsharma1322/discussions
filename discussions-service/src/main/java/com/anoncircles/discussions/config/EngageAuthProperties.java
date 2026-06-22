package com.anoncircles.discussions.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code engage.auth.*} from application.yml / env into a typed record.
 *
 * <p>{@code secret} must be at least 32 bytes (256 bits) — HMAC-SHA256 requires
 * key length ≥ algorithm output size. Use {@code openssl rand -base64 32} to
 * generate one for prod.
 */
@ConfigurationProperties(prefix = "engage.auth")
public record EngageAuthProperties(
        @NotBlank @Size(min = 32) String secret,
        Duration expiration
) {
    public EngageAuthProperties {
        if (expiration == null) {
            expiration = Duration.ofDays(7);
        }
    }
}
