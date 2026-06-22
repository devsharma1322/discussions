package com.anoncircles.discussions.security;

import com.anoncircles.discussions.domain.User;
import com.anoncircles.discussions.exception.InvalidTokenException;
import com.anoncircles.discussions.lib.EngageAuthTokenService;
import com.anoncircles.discussions.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Extracts the {@code engage-auth} JWT from the {@code Authorization: Bearer …}
 * header, verifies it, loads the user, and places an {@link EngageUserPrincipal}
 * on the Spring SecurityContext.
 *
 * <p>Rejection cases: missing / malformed / expired / invalid token, or the
 * user row no longer exists.
 *
 * <p>Skipped for {@code /actuator/health} and the public auth endpoints
 * ({@code POST /auth/session}, {@code POST /auth/logout}). Runs for
 * {@code GET /auth/me}.
 */
@Component
@RequiredArgsConstructor
public class EngageAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final EngageAuthTokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.startsWith("/actuator/health")) {
            return true;
        }
        // /auth/me needs the principal; everything else under /auth is public.
        if (path.equals("/auth/me")) {
            return false;
        }
        return path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        Claims claims = tokenService.verify(token);
        UUID userId = tokenService.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        EngageUserPrincipal principal = new EngageUserPrincipal(user.id(), user.displayName());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
