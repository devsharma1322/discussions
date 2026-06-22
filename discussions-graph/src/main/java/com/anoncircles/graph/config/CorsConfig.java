package com.anoncircles.graph.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Restricts CORS to the configured UI origin only.
 *
 * <p>In the {@code dev} profile we additionally whitelist Apollo Sandbox so
 * developers can hit the BFF from {@code studio.apollographql.com} without
 * the desktop app. Prod profile stays strict — only {@code graph.ui-origin}.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebFluxConfigurer corsConfigurer(@Value("${graph.ui-origin}") String uiOrigin,
                                            Environment env) {
        List<String> allowed = new ArrayList<>();
        allowed.add(uiOrigin);
        if (List.of(env.getActiveProfiles()).contains("dev")) {
            allowed.add("https://studio.apollographql.com");
            allowed.add("https://sandbox.apollo.dev");
        }
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/graphql/**")
                        .allowedOrigins(allowed.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}

