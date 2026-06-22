package com.anoncircles.discussions.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.*} from application.yml.
 *
 * <p>{@code baseUrl} is kept around for future external-link generation.
 * Anonymous flow doesn't need email/SMTP config.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl) {
}
