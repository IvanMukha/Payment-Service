package com.mukha.paymentservice.client;

import com.mukha.paymentservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${services.user-service.name}",
        url = "${services.user-service.url}")

public interface UserServiceClient {

    @GetMapping("/v1/api/users/{id}")
    UserResponse getUserById(@PathVariable Long id);
}
