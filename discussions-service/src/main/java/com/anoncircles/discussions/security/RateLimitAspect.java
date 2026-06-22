package com.anoncircles.discussions.security;

import com.anoncircles.discussions.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP aspect that enforces {@link RateLimited} per authenticated user id.
 *
 * <p>Buckets are keyed on {@code methodSignature + userId} so the limit is
 * scoped to one operation per user (and one user's actions never affect
 * another's bucket). Unauthenticated callers fall through; controllers that
 * carry {@code @RateLimited} are expected to also be behind the auth filter,
 * so an unauthenticated hit is a programming error and we fail loud.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof EngageUserPrincipal principal)) {
            throw new IllegalStateException("@RateLimited applied to an unauthenticated route");
        }

        String method = ((MethodSignature) joinPoint.getSignature()).getMethod().toGenericString();
        String key = method + "|" + principal.id();
        Bandwidth bandwidth = bandwidthFor(rateLimited);
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(bandwidth).build());

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for this operation",
                    rateLimited.maxPerHour() > 0 ? Duration.ofHours(1) : Duration.ofMinutes(1));
        }
        return joinPoint.proceed();
    }

    private static Bandwidth bandwidthFor(RateLimited annotation) {
        if (annotation.maxPerHour() > 0) {
            return Bandwidth.builder()
                    .capacity(annotation.maxPerHour())
                    .refillIntervally(annotation.maxPerHour(), Duration.ofHours(1))
                    .build();
        }
        if (annotation.maxPerMinute() > 0) {
            return Bandwidth.builder()
                    .capacity(annotation.maxPerMinute())
                    .refillIntervally(annotation.maxPerMinute(), Duration.ofMinutes(1))
                    .build();
        }
        throw new IllegalArgumentException(
                "@RateLimited requires maxPerHour or maxPerMinute > 0");
    }
}
