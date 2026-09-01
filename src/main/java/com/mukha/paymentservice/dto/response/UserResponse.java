package com.mukha.paymentservice.dto.response;

import java.util.UUID;

public record UserResponse(
        Long id,

        UUID keycloakUUID,

        String name,

        String surname,

        String email) {
}
