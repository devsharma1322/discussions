package com.anoncircles.genai.dto;

import com.anoncircles.genai.prompt.GenerateMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /generate}.
 *
 * <p>Cross-field check ({@code description} required when {@code mode} is
 * {@code FROM_TOPIC_AND_DESCRIPTION}) is enforced in the controller — Bean
 * Validation expresses single-field rules; the controller turns multi-field
 * violations into a 400.
 */
public record GenerateRequest(
        @NotBlank @Size(min = 3, max = 80) String topic,
        @Size(max = 500) String description,
        @NotNull GenerateMode mode
) {
}
