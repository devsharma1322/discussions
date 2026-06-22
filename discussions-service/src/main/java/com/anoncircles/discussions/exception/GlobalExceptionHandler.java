package com.anoncircles.discussions.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralises HTTP error responses. Keeps response payloads uniform and
 * removes the temptation for individual controllers to leak stack traces or
 * implementation details.
 *
 * <p>Mapping summary:
 * <ul>
 *   <li>{@code MethodArgumentNotValidException} → 400 (field-level errors)</li>
 *   <li>{@code HttpMessageNotReadableException} → 400 (e.g. unknown JSON field
 *       like {@code topic} in PATCH body — the field allowlist defence)</li>
 *   <li>{@code AccessDeniedException} → 403</li>
 *   <li>{@code TokenExpiredException} / {@code InvalidTokenException} → 401</li>
 *   <li>{@code RateLimitExceededException} → 429 with {@code Retry-After}</li>
 *   <li>{@code ResponseStatusException} → passed through with its declared status</li>
 *   <li>everything else → 500 with a sanitised body</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("fields", fieldErrors), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        String detail = "Malformed request body";
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof JsonMappingException jme) {
            detail = "Invalid request body: " + jme.getOriginalMessage();
        }
        return body(HttpStatus.BAD_REQUEST, detail, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "Forbidden", null, null);
    }

    @ExceptionHandler({TokenExpiredException.class, InvalidTokenException.class})
    public ResponseEntity<Map<String, Object>> handleAuthFailure(RuntimeException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Authentication required", null, null);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfter().toSeconds()));
        Map<String, Object> response = baseBody(HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage() == null ? "Too many requests" : ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return body(status, message, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null, null);
    }

    // ---- helpers ----

    private static ResponseEntity<Map<String, Object>> body(
            HttpStatus status, String message, Map<String, ?> extra, HttpHeaders headers) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.body(baseBody(status, message, extra));
    }

    private static Map<String, Object> baseBody(HttpStatus status, String message, Map<String, ?> extra) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        if (extra != null) {
            response.putAll(extra);
        }
        return response;
    }

    /** Kept for callers that want to surface a list rather than a map. */
    public static List<String> flatten(Map<String, String> fields) {
        return fields.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList();
    }
}
