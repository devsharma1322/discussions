package com.anoncircles.genai.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-source IP rate limiter — defence-in-depth ahead of the Gemini call.
 *
 * <p>60 req/min per source IP. {@code /actuator/health} is unmetered. Runs
 * just after {@link InternalAuthWebFilter} so unauthenticated traffic is
 * dropped before consuming bucket quota.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitWebFilter implements WebFilter {

    private static final int MAX_PER_MINUTE = 60;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getPath().value().startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        String key = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_PER_MINUTE)
                        .refillIntervally(MAX_PER_MINUTE, Duration.ofMinutes(1))
                        .build())
                .build());

        if (!bucket.tryConsume(1)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().put("Retry-After", List.of("60"));
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
