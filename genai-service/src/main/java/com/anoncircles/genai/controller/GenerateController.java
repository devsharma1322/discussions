package com.anoncircles.genai.controller;

import com.anoncircles.genai.dto.GenerateRequest;
import com.anoncircles.genai.prompt.GenerateMode;
import com.anoncircles.genai.prompt.PromptBuilder;
import com.anoncircles.genai.service.GenaiProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Streams a circle description as SSE events.
 *
 * <p>Wire format:
 * <pre>
 *   data: "Curious "
 *   data: "about "
 *   data: "java?..."
 *   event: done
 *   data:
 * </pre>
 *
 * <p>If the upstream provider fails (Gemini quota, safety block, network),
 * the stream emits a single {@code data: {"status":"UNAVAILABLE","message":"..."}}
 * event followed by {@code event: done}. No 5xx ever surfaces — the BFF
 * resolver maps the marker to the {@code DescriptionUnavailable} union branch
 * so the UI handles it at compile time.
 *
 * <p>Behind {@code InternalAuthWebFilter}; per-IP rate-limited by
 * {@code RateLimitWebFilter}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class GenerateController {

    /** Heartbeat keeps long generations from being killed by intermediate proxies. */
    private static final Duration HEARTBEAT = Duration.ofSeconds(15);

    private final PromptBuilder promptBuilder;
    private final GenaiProvider provider;

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generate(@Valid @RequestBody GenerateRequest request) {
        validateMode(request);

        PromptBuilder.Prompt prompt = promptBuilder.build(
                request.topic(), request.description(), request.mode());

        Flux<ServerSentEvent<String>> tokens = provider.stream(prompt)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());

        Flux<ServerSentEvent<String>> done = Flux.just(
                ServerSentEvent.<String>builder().event("done").data("").build());

        Flux<ServerSentEvent<String>> heartbeats = Flux.interval(HEARTBEAT)
                .map(i -> ServerSentEvent.<String>builder().comment("hb").build());

        return Flux.merge(tokens.concatWith(done), heartbeats.takeUntilOther(done))
                .onErrorResume(ex -> {
                    log.warn("genai stream failed: {}", ex.toString());
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .data("{\"status\":\"UNAVAILABLE\",\"message\":\"AI is unavailable right now\"}")
                                    .build(),
                            ServerSentEvent.<String>builder().event("done").data("").build()
                    );
                });
    }

    private static void validateMode(GenerateRequest request) {
        if (request.mode() == GenerateMode.FROM_TOPIC_AND_DESCRIPTION
                && (request.description() == null || request.description().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "description is required when mode is FROM_TOPIC_AND_DESCRIPTION");
        }
    }
}
