package com.anoncircles.genai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code genai.internal-auth.*} from application.yml. The token is a
 * shared secret between {@code discussions-graph} and {@code genai-service};
 * it authenticates the BFF as the service-to-service caller.
 */
@ConfigurationProperties(prefix = "genai.internal-auth")
public record InternalAuthProperties(
        @NotBlank String token
) {
}
