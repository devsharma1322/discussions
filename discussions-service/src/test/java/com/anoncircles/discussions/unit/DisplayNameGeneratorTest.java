package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.lib.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNameGeneratorTest {

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[A-Z][a-zA-Z]+-\\d{2}$");

    private final DisplayNameGenerator gen = new DisplayNameGenerator();

    @Test
    void generatesNonBlankName() {
        assertThat(gen.generate()).isNotBlank();
    }

    @Test
    void respectsAdjectiveAnimalSuffixFormat() {
        for (int i = 0; i < 100; i++) {
            assertThat(gen.generate()).matches(HANDLE_PATTERN);
        }
    }

    @Test
    void producesGoodSpreadAcrossManyInvocations() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(gen.generate());
        }
        // 64 × 64 × 100 = 409,600 possibilities. Random sample of 500 must
        // pick at least a few hundred distinct values; <50 would smell like
        // a broken RNG seed.
        assertThat(seen).hasSizeGreaterThan(300);
    }
}
