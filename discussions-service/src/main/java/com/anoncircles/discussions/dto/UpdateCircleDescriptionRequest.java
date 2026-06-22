package com.anoncircles.discussions.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Strict body for {@code PATCH /circles/{id}}. Only {@code description} is
 * accepted; any other field in the JSON body causes a 400 (because
 * {@code ignoreUnknown = false} flips Jackson's behaviour to fail).
 *
 * <p>Combined with the admin-only authorization check at the controller layer,
 * this enforces the rule that {@code topic} is immutable after circle creation.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateCircleDescriptionRequest(
        @NotBlank @Size(min = 1, max = 500) String description
) {
}
