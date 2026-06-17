package com.interviewcoach.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingAuthenticationReturnsUnauthorizedResponse() {
        var response = handler.handleMissingAuthentication(
                new AuthenticationCredentialsNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().message()).isEqualTo("Authentication required");
        assertThat(response.getBody().requestId()).isNotBlank();
    }

    @Test
    void genericExceptionKeepsInternalMessageOpaqueAndIncludesRequestId() {
        var response = handler.handleGeneric(new RuntimeException("backend failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().requestId()).isNotBlank();
    }
}
