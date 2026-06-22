package com.anoncircles.graph.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code graph.*} from application.yml. */
@ConfigurationProperties(prefix = "graph")
public record GraphProperties(
        @NotBlank String discussionsServiceUrl,
        @NotBlank String genaiServiceUrl,
        @NotBlank String internalAuthToken,
        @NotBlank String uiOrigin,
        Query query
) {
    public record Query(int maxDepth) {
    }
}
