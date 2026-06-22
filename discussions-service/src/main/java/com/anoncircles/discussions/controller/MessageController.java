package com.anoncircles.discussions.controller;

import com.anoncircles.discussions.dto.CreateMessageRequest;
import com.anoncircles.discussions.dto.MessageResponse;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.security.EngageUserPrincipal;
import com.anoncircles.discussions.security.RateLimited;
import com.anoncircles.discussions.service.MessageService;
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
 * REST endpoints for messages within a thread.
 *
 * <p>Membership in the parent circle is required to both read and post.
 * {@code POST} is rate-limited per user (30/min) by {@link RateLimited}.
 */
@RestController
@RequestMapping("/threads/{threadId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public PageResponse<MessageResponse> list(
            @PathVariable UUID threadId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal EngageUserPrincipal principal) {
        return messageService.list(
                threadId,
                principal.id(),
                PageResponse.normalizePage(page),
                PageResponse.clampLimit(limit));
    }

    @PostMapping
    @RateLimited(maxPerMinute = 30)
    public MessageResponse post(
            @PathVariable UUID threadId,
            @Valid @RequestBody CreateMessageRequest body,
            @AuthenticationPrincipal EngageUserPrincipal principal) {
        return messageService.post(threadId, body.body(), principal.id());
    }
}
