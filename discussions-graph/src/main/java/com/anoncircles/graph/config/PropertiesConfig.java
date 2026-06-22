package com.anoncircles.graph.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphProperties.class)
public class PropertiesConfig {
}
