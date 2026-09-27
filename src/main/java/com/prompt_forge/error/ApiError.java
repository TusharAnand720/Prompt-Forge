package com.prompt_forge.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Builder
public record ApiError(
        HttpStatus status,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ApiFieldError> errors,
        Instant timestamp) {

    public ApiError {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public ApiError(HttpStatus status, String message) {
        this(status, message, null, Instant.now());
    }

    public ApiError(HttpStatus status, List<ApiFieldError> errors, String message) {
        this(status, message, errors, Instant.now());
    }
}

@Builder
record ApiFieldError(
        String fieldName,
        String message) {
}
