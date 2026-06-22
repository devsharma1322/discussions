package com.anoncircles.discussions.controller;

import com.anoncircles.discussions.dto.CircleResponse;
import com.anoncircles.discussions.dto.CircleScope;
import com.anoncircles.discussions.dto.CircleSort;
import com.anoncircles.discussions.dto.CreateCircleRequest;
import com.anoncircles.discussions.dto.JoinResponse;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.dto.UpdateCircleDescriptionRequest;
import com.anoncircles.discussions.security.EngageUserPrincipal;
import com.anoncircles.discussions.security.RateLimited;
import com.anoncircles.discussions.service.CircleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for circles. All routes require auth ({@link EngageUserPrincipal});
 * mutation routes additionally enforce per-route rules (admin-only, rate limits).
 */
@RestController
@RequestMapping("/circles")
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circleService;

    @GetMapping
    public PageResponse<CircleResponse> list(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal EngageUserPrincipal principal) {

        int p = PageResponse.normalizePage(page);
        int l = PageResponse.clampLimit(limit);
        return circleService.list(
                CircleScope.parse(scope),
                CircleSort.parse(sort),
                search,
                principal.id(),
                p,
                l);
    }

    @GetMapping("/{id}")
    public CircleResponse get(@PathVariable UUID id,
                              @AuthenticationPrincipal EngageUserPrincipal principal) {
        return circleService.get(id, principal.id());
    }

    @PostMapping
    @RateLimited(maxPerHour = 5)
    public CircleResponse create(@Valid @RequestBody CreateCircleRequest body,
                                 @AuthenticationPrincipal EngageUserPrincipal principal) {
        return circleService.create(body.topic(), body.description(), principal.id());
    }

    @PatchMapping("/{id}")
    public CircleResponse update(@PathVariable UUID id,
                                 @Valid @RequestBody UpdateCircleDescriptionRequest body,
                                 @AuthenticationPrincipal EngageUserPrincipal principal) {
        return circleService.updateDescription(id, body.description(), principal.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal EngageUserPrincipal principal) {
        circleService.delete(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public JoinResponse join(@PathVariable UUID id,
                             @AuthenticationPrincipal EngageUserPrincipal principal) {
        return circleService.join(id, principal.id());
    }

    @DeleteMapping("/{id}/join")
    public ResponseEntity<Void> leave(@PathVariable UUID id,
                                      @AuthenticationPrincipal EngageUserPrincipal principal) {
        circleService.leave(id, principal.id());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
