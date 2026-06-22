package com.anoncircles.discussions.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security wiring for the pure-anonymous flow:
 *
 * <ul>
 *   <li>Stateless session policy (every request authenticates via the JWT filter).</li>
 *   <li>CSRF disabled.</li>
 *   <li>{@code POST /auth/session}, {@code POST /auth/logout},
 *       {@code /actuator/health/**}, and CORS preflights are public.</li>
 *   <li>{@code GET /auth/me} requires authentication.</li>
 *   <li>Everything else requires authentication.</li>
 *   <li>{@link EngageAuthFilter} runs ahead of {@link UsernamePasswordAuthenticationFilter}.</li>
 *   <li>CORS restricted to the configured UI origin.</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final EngageAuthFilter engageAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers("/auth/**", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(engageAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * The @Component-registered filter would otherwise be auto-registered as a
     * top-level servlet filter and run twice per request.
     */
    @Bean
    public FilterRegistrationBean<EngageAuthFilter> disableEngageAuthFilterAutoRegistration(
            EngageAuthFilter filter) {
        FilterRegistrationBean<EngageAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
