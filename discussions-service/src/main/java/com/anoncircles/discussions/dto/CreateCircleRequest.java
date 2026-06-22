package com.anoncircles.discussions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCircleRequest(
        @NotBlank @Size(min = 3, max = 80) String topic,
        @NotBlank @Size(min = 1, max = 500) String description
) {
}
