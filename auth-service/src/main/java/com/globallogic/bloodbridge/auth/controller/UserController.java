package com.globallogic.bloodbridge.auth.controller;

import com.globallogic.bloodbridge.auth.dto.UserResponse;
import com.globallogic.bloodbridge.auth.model.Role;
import com.globallogic.bloodbridge.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam(required = false) Role role) {
        if (role != null) {
            return ResponseEntity.ok(authService.listUsersByRole(role));
        }
        return ResponseEntity.ok(authService.listAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
