package com.globallogic.bloodbridge.notification.client;

import com.globallogic.bloodbridge.notification.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);
}
