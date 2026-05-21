package com.interviewcoach.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(TargetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTargetNotFound(TargetNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("TARGET_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("PROFILE_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", generateRequestId()));
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
