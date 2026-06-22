package com.anoncircles.graph.security;

import com.anoncircles.graph.context.GraphContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process per-user rate limiter for the {@code generateDescription}
 * mutation. 10 calls/hour per anonymous user.
 *
 * <p>Keyed on the JWT's {@code sub} claim (user id). The signature is NOT
 * verified here — the BFF doesn't hold the JWT secret. Either the claim is
 * valid (the request will succeed downstream) or it's bogus (the request
 * will 401 downstream, having consumed at most one slot in an attacker-owned
 * bucket). Real production limits would live in Redis keyed off the verified
 * principal; this is sufficient for the dev/demo footprint.
 *
 * @see com.anoncircles.graph.resolver.GenaiResolver
 */
@Component
public class GenerateRateLimiter {

    private static final int MAX_PER_HOUR = 10;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @return true if the call was allowed (token consumed); false if the
     *         caller has exhausted their quota.
     */
    public boolean tryAcquire(GraphContext ctx) {
        String key = keyFor(ctx);
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_PER_HOUR)
                        .refillIntervally(MAX_PER_HOUR, Duration.ofHours(1))
                        .build())
                .build());
        return bucket.tryConsume(1);
    }

    /**
     * Read the JWT's {@code sub} claim without verifying the signature.
     * Falls back to the raw token if the JWT is malformed.
     */
    private String keyFor(GraphContext ctx) {
        String token = ctx.engageAuth();
        if (token == null) return "anon";
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
                JsonNode node = mapper.readTree(new String(payload, StandardCharsets.UTF_8));
                JsonNode sub = node.get("sub");
                if (sub != null && sub.isTextual()) return sub.asText();
            }
        } catch (Exception ignored) {
            /* fall through */
        }
        return "raw:" + token;
    }
}
