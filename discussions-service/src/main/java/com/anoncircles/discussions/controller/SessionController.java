package com.anoncircles.discussions.controller;

import com.anoncircles.discussions.dto.SessionResponse;
import com.anoncircles.discussions.dto.UserResponse;
import com.anoncircles.discussions.security.EngageUserPrincipal;
import com.anoncircles.discussions.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST endpoints for the anonymous session lifecycle:
 *
 * <ul>
 *   <li>{@code POST /auth/session} — public; creates a fresh anonymous user
 *       and returns {@code { engageAuth, user }}.</li>
 *   <li>{@code GET /auth/me} — requires auth; returns the current user.</li>
 *   <li>{@code POST /auth/logout} — no-op on the server (the JWT is stateless).
 *       Client clears local storage. 204.</li>
 * </ul>
 *
 * <p>{@code /auth/me} is the only route under {@code /auth/} that the
 * {@code EngageAuthFilter} runs on; the rest are public-by-design (a brand-new
 * visitor needs to hit {@code POST /auth/session} without any token).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/session")
    public SessionResponse startSession() {
        return sessionService.createSession();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal EngageUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return sessionService.me(principal.id());
    }

    /**
     * Stateless JWT — there's nothing to invalidate server-side without a
     * blacklist. Returns 204 so clients have an explicit success signal.
     * Anonymous flow makes "logout" really mean "drop your identity"; the
     * client is expected to clear local storage and call
     * {@code POST /auth/session} again for a new identity.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
