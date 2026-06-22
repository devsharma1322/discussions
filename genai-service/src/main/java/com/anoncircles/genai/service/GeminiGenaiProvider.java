package com.anoncircles.genai.service;

import com.anoncircles.genai.prompt.PromptBuilder;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Gemini-backed {@link GenaiProvider} — the sole implementation. There is no
 * fallback "mock" provider; the service requires {@code GEMINI_API_KEY} to be
 * set at startup and fails fast if it isn't.
 *
 * <p>Streams tokens from {@code gemini-3.5-flash} (overridable via
 * {@code GEMINI_MODEL}) using the official {@code com.google.genai} SDK.
 * The SDK exposes a blocking {@link ResponseStream} iterator, so calls run
 * on {@link Schedulers#boundedElastic()} and emit each chunk through a Flux —
 * preserving the controller's SSE streaming semantics with no buffering.
 *
 * <p>Two streaming quirks are normalised here:
 * <ul>
 *   <li>Word-boundary whitespace: Gemini occasionally drops the space
 *       between two chunks (e.g., {@code "welcoming "} + {@code "space"}
 *       collapses to {@code "welcomingspace"} when concatenated). We inject
 *       a single space whenever both sides of the boundary are alphanumeric.</li>
 *   <li>Hard 300-char cap: even though the prompt requests ≤300 chars, the
 *       model sometimes overshoots. We truncate during streaming and complete
 *       the stream early when the cumulative byte count would exceed the cap.</li>
 * </ul>
 *
 * <p>Upstream failures (network, quota, auth) propagate as {@code Flux.error}
 * so the controller's {@code onErrorResume} branch can emit a single
 * {@code {"status":"UNAVAILABLE"}} event and complete cleanly. The exception
 * message is intentionally generic (not the raw upstream error) to avoid
 * leaking API details to downstream callers.
 */
@Slf4j
@Component
public class GeminiGenaiProvider implements GenaiProvider {

    private final Client client;
    private final String model;

    public GeminiGenaiProvider(
            @Value("${genai.gemini.api-key:}") String apiKey,
            @Value("${genai.gemini.model:gemini-3.5-flash}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is required — set it in genai-service/.env or as an env var.");
        }
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = model;
        log.info("GeminiGenaiProvider active — model={} (key length={})", model, apiKey.length());
    }

    @Override
    public Flux<String> stream(PromptBuilder.Prompt prompt) {
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(prompt.userMessage())))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(prompt.systemInstruction())))
                .build();

        return Flux.<String>create(sink -> {
            // Gemini SDK chunks sometimes drop the whitespace at their
            // boundary (so "welcoming " + "space" arrives as "welcoming"
            // + "space" → "welcomingspace" when the UI concatenates). We
            // track the tail of the previous chunk and inject a single
            // space whenever both sides of the boundary are alphanumeric.
            //
            // We also enforce a hard 300-character cap during streaming —
            // when the cumulative byte count would exceed it, we emit a
            // truncated final chunk and complete the stream early. This
            // matches the UI's textarea maxLength + protects the BFF from
            // unbounded payloads if Gemini ignores the prompt-level limit.
            final int MAX_CHARS = 300;
            int[] emitted = {0};
            String[] tail = {""};
            try (ResponseStream<GenerateContentResponse> stream =
                         client.models.generateContentStream(model, List.of(userContent), config)) {
                for (GenerateContentResponse response : stream) {
                    if (sink.isCancelled()) return;
                    String text = response.text();
                    if (text == null || text.isEmpty()) continue;

                    if (!tail[0].isEmpty() && needsSpaceBetween(tail[0], text)) {
                        text = " " + text;
                    }

                    if (emitted[0] + text.length() > MAX_CHARS) {
                        String truncated = text.substring(0, MAX_CHARS - emitted[0]);
                        if (!truncated.isEmpty()) sink.next(truncated);
                        sink.complete();
                        return;
                    }
                    sink.next(text);
                    emitted[0] += text.length();
                    tail[0] = text;
                }
                sink.complete();
            } catch (Exception ex) {
                log.warn("Gemini streaming failed: {}", ex.getMessage());
                sink.error(new GenaiUpstreamException("Gemini upstream failed", ex));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static boolean needsSpaceBetween(String prev, String next) {
        char prevEnd = prev.charAt(prev.length() - 1);
        char nextStart = next.charAt(0);
        return Character.isLetterOrDigit(prevEnd) && Character.isLetterOrDigit(nextStart);
    }

    @PreDestroy
    void shutdown() {
        try {
            client.close();
        } catch (Exception ignored) {
            // best-effort close
        }
    }
}
