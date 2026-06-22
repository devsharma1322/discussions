package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.lib.HandleGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HandleGeneratorTest {

    private final HandleGenerator gen = new HandleGenerator();
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[A-Z][a-zA-Z]+\\-\\d{2}$");

    @Test
    void deterministicForSameUserAndCircle() {
        UUID u = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertThat(gen.generate(u, c)).isEqualTo(gen.generate(u, c));
    }

    @Test
    void differentForSameUserDifferentCircles() {
        UUID u = UUID.randomUUID();
        Set<String> handles = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            handles.add(gen.generate(u, UUID.randomUUID()));
        }
        // Across 20 circles for one user, we expect lots of distinct handles.
        // (Not 20 — collisions are possible — but well above 10.)
        assertThat(handles).hasSizeGreaterThan(10);
    }

    @Test
    void differentForSameCircleDifferentUsers() {
        UUID c = UUID.randomUUID();
        Set<String> handles = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            handles.add(gen.generate(UUID.randomUUID(), c));
        }
        assertThat(handles).hasSizeGreaterThan(10);
    }

    @Test
    void handleMatchesFormat() {
        for (int i = 0; i < 50; i++) {
            String h = gen.generate(UUID.randomUUID(), UUID.randomUUID());
            assertThat(h).matches(HANDLE_PATTERN);
        }
    }
}
