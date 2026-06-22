package com.anoncircles.genai.unit;

import com.anoncircles.genai.prompt.GenerateMode;
import com.anoncircles.genai.prompt.PromptBuilder;
import com.anoncircles.genai.prompt.PromptBuilder.Prompt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the production-grade guardrail prompt. We don't assert the
 * entire system-instruction body verbatim (it would make every prompt tweak
 * a test failure for no real safety benefit); instead we pin down the
 * load-bearing rules — output language, 300-char limit, banned vocabulary
 * list, safety-gate keywords — and assert the user-message shape contains
 * the attendee inputs in the expected "Topic: ... Description: ... Output:"
 * layout.
 */
class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void fromTopic_includesTopicAndOmitsDescription() {
        Prompt p = builder.build("rust", null, GenerateMode.FROM_TOPIC);
        assertThat(p.userMessage())
                .startsWith("Topic: rust")
                .contains("Description: (none)")
                .endsWith("Output:");
    }

    @Test
    void fromTopicAndDescription_includesBoth() {
        Prompt p = builder.build("haskell", "lambda land", GenerateMode.FROM_TOPIC_AND_DESCRIPTION);
        assertThat(p.userMessage())
                .contains("Topic: haskell")
                .contains("Description: lambda land");
    }

    @Test
    void blankDescription_inPolishMode_collapsesToNone() {
        Prompt p = builder.build("topic", "  ", GenerateMode.FROM_TOPIC_AND_DESCRIPTION);
        assertThat(p.userMessage())
                .contains("Description: (none)")
                .doesNotContain("null");
    }

    @Test
    void nullDescription_inPolishMode_collapsesToNone() {
        Prompt p = builder.build("topic", null, GenerateMode.FROM_TOPIC_AND_DESCRIPTION);
        assertThat(p.userMessage())
                .contains("Description: (none)")
                .doesNotContain("null");
    }

    @Test
    void systemInstruction_enforces300CharLimit() {
        Prompt p = builder.build("anything", null, GenerateMode.FROM_TOPIC);
        assertThat(p.systemInstruction()).contains("Maximum 300 characters");
    }

    @Test
    void systemInstruction_lockOutputLanguageToEnglish() {
        Prompt p = builder.build("anything", null, GenerateMode.FROM_TOPIC);
        assertThat(p.systemInstruction())
                .contains("MUST be English")
                .contains("CRITICAL RULE — OUTPUT LANGUAGE");
    }

    @Test
    void systemInstruction_bannedVocabularyIncludesJoinUsAndExciting() {
        Prompt p = builder.build("anything", null, GenerateMode.FROM_TOPIC);
        assertThat(p.systemInstruction())
                .contains("Join us")
                .contains("exciting")
                .contains("Hope to see you");
    }

    @Test
    void systemInstruction_safetyGateMentionsHateSpeechAndInjection() {
        Prompt p = builder.build("anything", null, GenerateMode.FROM_TOPIC);
        assertThat(p.systemInstruction())
                .contains("SAFETY GATE")
                .contains("ADVERSARIAL GATE")
                .contains("Hate speech")
                .contains("prompt injection");
    }

    @Test
    void longDraftIsPassedThroughUnTruncated() {
        String draft = "x".repeat(500);
        Prompt p = builder.build("topic", draft, GenerateMode.FROM_TOPIC_AND_DESCRIPTION);
        assertThat(p.userMessage()).contains(draft);
    }
}
