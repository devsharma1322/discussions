package com.anoncircles.discussions.controller;

import com.anoncircles.discussions.dto.CreateThreadRequest;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.dto.ThreadResponse;
import com.anoncircles.discussions.security.EngageUserPrincipal;
import com.anoncircles.discussions.service.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for threads nested under a circle.
 *
 * <p>Mounted at {@code /circles/{circleId}/threads}. There is also a
 * top-level lookup at {@code GET /threads/{id}} so message-post flows can
 * reference a thread by id without knowing the circle.
 */
@RestController
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;

    @GetMapping("/circles/{circleId}/threads")
    public PageResponse<ThreadResponse> list(
            @PathVariable UUID circleId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return threadService.list(
                circleId,
                PageResponse.normalizePage(page),
                PageResponse.clampLimit(limit));
    }

    @PostMapping("/circles/{circleId}/threads")
    public ThreadResponse create(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateThreadRequest body,
            @AuthenticationPrincipal EngageUserPrincipal principal) {
        return threadService.create(circleId, body.title(), principal.id());
    }

    /**
     * Convenience: look up a thread by id directly. Useful for the UI's
     * {@code /threads/:id} route where the circle id isn't in the URL.
     */
    @GetMapping("/threads/{id}")
    public ThreadResponse get(@PathVariable UUID id) {
        return threadService.get(id);
    }
}
