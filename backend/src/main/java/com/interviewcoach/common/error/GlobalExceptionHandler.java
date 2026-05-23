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

    @ExceptionHandler(JobBriefNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobBriefNotFound(JobBriefNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("JOB_BRIEF_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssessmentNotFound(AssessmentNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("ASSESSMENT_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("REPORT_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(TrainingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrainingNotFound(TrainingNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("TRAINING_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(MockInterviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMockInterviewNotFound(MockInterviewNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("MOCK_INTERVIEW_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(AiProviderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderNotFound(AiProviderNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("AI_PROVIDER_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(AiProviderCallFailedException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderCallFailed(AiProviderCallFailedException ex) {
        return ResponseEntity.status(502)
                .body(new ErrorResponse("AI_PROVIDER_CALL_FAILED", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(AiParseException.class)
    public ResponseEntity<ErrorResponse> handleAiParseFailed(AiParseException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("AI_PARSE_FAILED", ex.getMessage(), generateRequestId()));
    }

    @ExceptionHandler(AppleAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleAppleAuthFailed(AppleAuthFailedException ex) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("APPLE_AUTH_FAILED", ex.getMessage(), generateRequestId()));
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
