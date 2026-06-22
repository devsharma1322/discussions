package com.anoncircles.discussions.dto;

/**
 * Generic envelope for endpoints that need to confirm completion without
 * returning a payload (logout, forgot-password, delete, etc.). Use
 * {@link #ok()} for the no-message case and {@link #ok(String)} when a
 * confirmation copy is appropriate.
 */
public record GenericResponse(boolean success, String message) {

    public static GenericResponse ok() {
        return new GenericResponse(true, null);
    }

    public static GenericResponse ok(String message) {
        return new GenericResponse(true, message);
    }
}
