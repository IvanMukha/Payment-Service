package com.mukha.paymentservice.unittest;

import com.mukha.paymentservice.client.UserServiceClient;
import com.mukha.paymentservice.dto.response.UserResponse;
import com.mukha.paymentservice.security.PaymentSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSecurityTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PaymentSecurity paymentSecurity;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    @Test
    void isOwner_ShouldReturnTrue_WhenUserIsOwner() {
        Long userId = 1L;
        String userUuid = UUID.randomUUID().toString();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("sub")).thenReturn(userUuid);

        UserResponse mockUser = mock(UserResponse.class);
        when(mockUser.keycloakUUID()).thenReturn(UUID.fromString(userUuid));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);

        boolean result = paymentSecurity.isOwner(userId);

        assertTrue(result);
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenUserIsNotOwner() {
        Long userId = 1L;
        String currentUserUuid = UUID.randomUUID().toString();
        String differentUserUuid = UUID.randomUUID().toString();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("sub")).thenReturn(currentUserUuid);

        UserResponse mockUser = mock(UserResponse.class);
        when(mockUser.keycloakUUID()).thenReturn(UUID.fromString(differentUserUuid));
        when(userServiceClient.getUserById(userId)).thenReturn(mockUser);

        boolean result = paymentSecurity.isOwner(userId);

        assertFalse(result);
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = paymentSecurity.isOwner(1L);

        assertFalse(result);
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenPrincipalIsNotJwt() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("not-a-jwt-instance");

        boolean result = paymentSecurity.isOwner(1L);

        assertFalse(result);
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenSubClaimMissing() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("sub")).thenReturn(null);

        boolean result = paymentSecurity.isOwner(1L);

        assertFalse(result);
    }
}

