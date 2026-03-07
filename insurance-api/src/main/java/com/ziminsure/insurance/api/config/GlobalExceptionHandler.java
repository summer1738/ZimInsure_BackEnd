package com.ziminsure.insurance.api.config;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Returns proper JSON error responses for constraint violations so the client
 * receives 409 with a clear message instead of 500 and redirect to /error (403).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Duplicate or invalid data";
        // Identify which unique constraint failed from "Duplicate entry 'value' for key '...'"
        if (message.contains("Duplicate entry")) {
            int start = message.indexOf("Duplicate entry '");
            if (start >= 0) {
                start += 17;
                int end = message.indexOf("'", start);
                String value = end > start ? message.substring(start, end) : "";
                if (value.contains("@")) {
                    message = "Email already in use. Please use a different email.";
                } else {
                    message = "ID number already in use. Please use a different ID number.";
                }
            } else {
                message = "A record with this value already exists. Please use unique email and ID number.";
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
    }
}
