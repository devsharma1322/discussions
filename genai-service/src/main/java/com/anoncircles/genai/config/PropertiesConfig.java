package com.anoncircles.genai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InternalAuthProperties.class)
public class PropertiesConfig {
}
