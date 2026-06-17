package com.interviewcoach.common.security;

import com.interviewcoach.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserReturnsAuthenticatedUserPrincipal() {
        User user = new User();
        user.setUsername("security-utils-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        assertThat(SecurityUtils.currentUser()).isSameAs(user);
    }

    @Test
    void currentUserFailsWithControlledExceptionWhenAuthenticationIsMissing() {
        assertThatThrownBy(SecurityUtils::currentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Authenticated user principal is missing");
    }

    @Test
    void currentUserFailsWithControlledExceptionWhenPrincipalIsNotUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null));

        assertThatThrownBy(SecurityUtils::currentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Authenticated user principal is missing");
    }
}
