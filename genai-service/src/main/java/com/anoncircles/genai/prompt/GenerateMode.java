package com.anoncircles.genai.prompt;

/**
 * Generation mode for {@link PromptBuilder}.
 *
 * <p>Explicit enum (not inferred from "is description empty?") so the API
 * surface fails loudly when callers forget to supply a field — see Spike
 * Appendix A §9 ("explicit modes beat inferred behaviour").
 */
public enum GenerateMode {
    FROM_TOPIC,
    FROM_TOPIC_AND_DESCRIPTION
}
