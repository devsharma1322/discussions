package com.anoncircles.discussions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateThreadRequest(
        @NotBlank @Size(min = 3, max = 120) String title
) {
}
