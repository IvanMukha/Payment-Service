package com.mukha.paymentservice.security;

import com.mukha.paymentservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {
    private final UserServiceClient userServiceClient;

    public boolean isOwner(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        String currentUserUuid = jwt.getClaim("sub");
        if (currentUserUuid == null) {
            return false;
        }
        String dbUserUuid = String.valueOf(userServiceClient.getUserById(userId).keycloakUUID());
        return currentUserUuid.equals(dbUserUuid);
    }
}
