package com.anoncircles.discussions.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all {@code @ConfigurationProperties} records the service uses.
 */
@Configuration
@EnableConfigurationProperties({EngageAuthProperties.class, AppProperties.class})
public class PropertiesConfig {
}
