package com.anoncircles.discussions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank @Size(min = 1, max = 2000) String body
) {
}
