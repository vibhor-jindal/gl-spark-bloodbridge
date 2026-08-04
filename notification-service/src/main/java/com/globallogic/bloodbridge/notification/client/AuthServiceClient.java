package com.globallogic.bloodbridge.notification.client;

import com.globallogic.bloodbridge.notification.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

    @GetMapping("/api/auth/users")
    List<UserDto> listUsers(@RequestParam(value = "role", required = false) String role);
}
