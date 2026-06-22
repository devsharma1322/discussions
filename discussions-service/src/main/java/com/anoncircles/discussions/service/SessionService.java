package com.anoncircles.discussions.service;

import com.anoncircles.discussions.domain.User;
import com.anoncircles.discussions.dto.SessionResponse;
import com.anoncircles.discussions.dto.UserResponse;
import com.anoncircles.discussions.lib.DisplayNameGenerator;
import com.anoncircles.discussions.lib.EngageAuthTokenService;
import com.anoncircles.discussions.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Spawns anonymous sessions and answers "who am I" queries.
 *
 * <p>Replaces the old auth flow (register / verify / login / forgot / reset):
 * a {@code POST /auth/session} call inserts a new {@link User} with a randomly
 * generated display name, signs the engage-auth JWT, and returns both. The
 * client persists the JWT to localStorage — the same JWT proves identity on
 * every subsequent request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserRepository userRepository;
    private final DisplayNameGenerator displayNameGenerator;
    private final EngageAuthTokenService engageAuthTokenService;

    public SessionResponse createSession() {
        String displayName = displayNameGenerator.generate();
        User user = userRepository.insert(displayName);
        String engageAuth = engageAuthTokenService.sign(user.id());
        log.info("Created anonymous session for user={} as '{}'", user.id(), displayName);
        return new SessionResponse(engageAuth, UserResponse.from(user));
    }

    public UserResponse me(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
