package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.domain.User;
import com.anoncircles.discussions.dto.SessionResponse;
import com.anoncircles.discussions.dto.UserResponse;
import com.anoncircles.discussions.lib.DisplayNameGenerator;
import com.anoncircles.discussions.lib.EngageAuthTokenService;
import com.anoncircles.discussions.repository.UserRepository;
import com.anoncircles.discussions.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private UserRepository users;
    private DisplayNameGenerator names;
    private EngageAuthTokenService tokens;
    private SessionService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        names = mock(DisplayNameGenerator.class);
        tokens = mock(EngageAuthTokenService.class);
        service = new SessionService(users, names, tokens);
    }

    @Test
    void createSession_assignsRandomName_andSignsToken() {
        when(names.generate()).thenReturn("BoldFox-42");
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        when(users.insert("BoldFox-42")).thenReturn(new User(id, "BoldFox-42", now));
        when(tokens.sign(id)).thenReturn("signed.jwt.value");

        SessionResponse response = service.createSession();

        assertThat(response.engageAuth()).isEqualTo("signed.jwt.value");
        assertThat(response.user().id()).isEqualTo(id);
        assertThat(response.user().displayName()).isEqualTo("BoldFox-42");
        verify(users, times(1)).insert("BoldFox-42");
    }

    @Test
    void me_returnsExistingUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(new User(id, "QuietOwl-09", Instant.now())));
        UserResponse u = service.me(id);
        assertThat(u.id()).isEqualTo(id);
        assertThat(u.displayName()).isEqualTo("QuietOwl-09");
    }

    @Test
    void me_throwsUnauthorizedIfMissing() {
        UUID id = UUID.randomUUID();
        when(users.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.me(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authentication");
    }
}
