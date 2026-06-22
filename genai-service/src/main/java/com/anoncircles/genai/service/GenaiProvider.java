package com.anoncircles.genai.service;

import com.anoncircles.genai.prompt.PromptBuilder;
import reactor.core.publisher.Flux;

/**
 * Streams a description back token-by-token.
 *
 * <p>Single implementation: {@code GeminiGenaiProvider} (Google Gen AI Java
 * SDK on {@code gemini-3.5-flash}). The service fails fast at startup if
 * {@code GEMINI_API_KEY} is missing — there is no mock fallback.
 *
 * <p>Implementations <strong>must not</strong> surface upstream failures — wrap
 * them as {@link reactor.core.publisher.Flux#error(Throwable)} so the
 * controller's {@code onErrorResume} can emit a single
 * {@code {"status":"UNAVAILABLE"}} SSE event and complete cleanly.
 */
public interface GenaiProvider {
    Flux<String> stream(PromptBuilder.Prompt prompt);
}
