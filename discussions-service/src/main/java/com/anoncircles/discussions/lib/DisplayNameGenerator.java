package com.anoncircles.discussions.lib;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates a random anonymous display name like {@code ShyOtter-42}.
 *
 * <p>Used at session-creation time to give every new visitor a friendly,
 * pseudonymous identity. Unlike {@link HandleGenerator} (which is deterministic
 * in {@code userId+circleId}), this generator is purely random — names are
 * persisted on the {@code users.display_name} column.
 */
@Component
public class DisplayNameGenerator {

    private final SecureRandom rng = new SecureRandom();

    public String generate() {
        String adjective = AnimalWords.ADJECTIVES.get(rng.nextInt(AnimalWords.ADJECTIVES.size()));
        String animal = AnimalWords.ANIMALS.get(rng.nextInt(AnimalWords.ANIMALS.size()));
        int suffix = rng.nextInt(100);
        return "%s%s-%02d".formatted(adjective, animal, suffix);
    }
}
