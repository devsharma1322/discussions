package com.anoncircles.discussions.lib;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

/**
 * Generates a deterministic per-circle pseudonym of the form
 * {@code <Adjective><Animal>-NN} — e.g. {@code PurpleHeron-42}.
 *
 * <p>Deterministic in {@code (userId, circleId)} so a user always sees the
 * same handle in a given circle across sessions, but the handle differs
 * between circles. Seed = first 8 bytes of {@code SHA-256(userId || circleId)}.
 *
 * <p>Identity is consistent within a circle but uncorrelatable across circles.
 */
@Component
public class HandleGenerator {

    public String generate(UUID userId, UUID circleId) {
        long seed = seedFor(userId, circleId);
        Random rng = new Random(seed);

        String adjective = AnimalWords.ADJECTIVES.get(rng.nextInt(AnimalWords.ADJECTIVES.size()));
        String animal = AnimalWords.ANIMALS.get(rng.nextInt(AnimalWords.ANIMALS.size()));
        int suffix = rng.nextInt(100);
        return "%s%s-%02d".formatted(adjective, animal, suffix);
    }

    /** Visible for unit tests. */
    static long seedFor(UUID userId, UUID circleId) {
        String key = userId.toString() + "|" + circleId.toString();
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", ex);
        }
        byte[] digest = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(digest, 0, 8).getLong();
    }
}
