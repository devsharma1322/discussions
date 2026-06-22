package com.anoncircles.discussions.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as rate-limited per authenticated user. The
 * {@code RateLimitAspect} enforces the quota using Bucket4j.
 *
 * <p>Examples from the spike:
 * <ul>
 *   <li>{@code @RateLimited(maxPerHour = 5)} on {@code POST /circles}</li>
 *   <li>{@code @RateLimited(maxPerMinute = 30)} on {@code POST /threads/{id}/messages}</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /** Maximum allowed invocations per hour per user (0 = disabled). */
    int maxPerHour() default 0;

    /** Maximum allowed invocations per minute per user (0 = disabled). */
    int maxPerMinute() default 0;
}
