package com.anoncircles.graph.unit;

import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.security.GenerateRateLimiter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateRateLimiterTest {

    private final GenerateRateLimiter limiter = new GenerateRateLimiter();

    @Test
    void allowsUpTo10CallsPerUser_thenBlocks() {
        GraphContext ctx = ctxFor("user-A");
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(ctx)).isTrue();
        }
        assertThat(limiter.tryAcquire(ctx)).isFalse();
    }

    @Test
    void bucketsArePerUser_notGlobal() {
        GraphContext a = ctxFor("user-A");
        GraphContext b = ctxFor("user-B");
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire(a);
        }
        assertThat(limiter.tryAcquire(a)).isFalse();
        assertThat(limiter.tryAcquire(b)).isTrue();
    }

    @Test
    void unparseableTokenStillProducesAStableKey() {
        GraphContext ctx = new GraphContext("not-a-jwt");
        assertThat(limiter.tryAcquire(ctx)).isTrue();
    }

    private static GraphContext ctxFor(String sub) {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"sub\":\"" + sub + "\"}");
        String token = header + "." + payload + ".unused";
        return new GraphContext(token);
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
