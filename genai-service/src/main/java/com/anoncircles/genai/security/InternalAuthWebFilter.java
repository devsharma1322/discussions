package com.anoncircles.genai.security;

import com.anoncircles.genai.config.InternalAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reactive WebFilter that protects every route except {@code /actuator/health}
 * with a shared-secret header.
 *
 * <p>Compares {@code X-Internal-Auth} against
 * {@link InternalAuthProperties#token()} using {@link MessageDigest#isEqual},
 * which is constant-time and immune to header-guessing via timing side channels.
 * Returns 401 on missing/mismatch without echoing the header value.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalAuthWebFilter implements WebFilter {

    public static final String HEADER = "X-Internal-Auth";

    private final byte[] expectedToken;

    public InternalAuthWebFilter(InternalAuthProperties props) {
        this.expectedToken = props.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isPublicPath(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String provided = request.getHeaders().getFirst(HEADER);
        if (provided == null || !constantTimeEquals(provided.getBytes(StandardCharsets.UTF_8), expectedToken)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        return chain.filter(exchange);
    }

    private static boolean isPublicPath(String path) {
        return path.startsWith("/actuator/health");
    }

    /**
     * MessageDigest.isEqual is documented as constant-time across length
     * mismatches in modern JDKs; we still compare to the expected length first
     * to be explicit.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            // Run the compare anyway against a same-length dummy to keep timing flat.
            MessageDigest.isEqual(b, b);
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
