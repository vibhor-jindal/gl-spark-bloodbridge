package com.globallogic.bloodbridge.auth.service;

import com.globallogic.bloodbridge.auth.dto.AuthResponse;
import com.globallogic.bloodbridge.auth.dto.LoginRequest;
import com.globallogic.bloodbridge.auth.dto.RegisterRequest;
import com.globallogic.bloodbridge.auth.exception.DuplicateUserException;
import com.globallogic.bloodbridge.auth.exception.InvalidCredentialsException;
import com.globallogic.bloodbridge.auth.model.Role;
import com.globallogic.bloodbridge.auth.model.User;
import com.globallogic.bloodbridge.auth.repository.UserRepository;
import com.globallogic.bloodbridge.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}, written test-first (TDD).
 * Covers US-006 acceptance criteria: token issuance on valid login,
 * 401-equivalent behavior on bad credentials, duplicate-email rejection,
 * and that passwords are never persisted in plain text.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Asha Verma", "asha@example.com", "SecurePass123", Role.DONOR);

        savedUser = User.builder()
                .userId(1L)
                .fullName("Asha Verma")
                .email("asha@example.com")
                .passwordHash("$2a$10$hashedvalueplaceholder")
                .role(Role.DONOR)
                .build();
    }

    @Test
    @DisplayName("US-006 AC4: Registering a new user stores a BCrypt hash, never the plain-text password")
    void testRegister_Success_HashesPassword() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$10$hashedvalueplaceholder");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(1L, "asha@example.com", Role.DONOR)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("signed.jwt.token");
        assertThat(response.getRole()).isEqualTo(Role.DONOR);
        verify(passwordEncoder, times(1)).encode("SecurePass123");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("$2a$10$hashedvalueplaceholder")
                && !u.getPasswordHash().equals("SecurePass123")));
    }

    @Test
    @DisplayName("US-006: Registering with an email already on file is rejected")
    void testRegister_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-006 AC1: Valid credentials issue a signed JWT containing the user's ID and role")
    void testLogin_ValidCredentials_IssuesToken() {
        LoginRequest loginRequest = new LoginRequest("asha@example.com", "SecurePass123");

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("SecurePass123", savedUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(1L, "asha@example.com", Role.DONOR)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("signed.jwt.token");
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(jwtService).generateToken(eq(1L), eq("asha@example.com"), eq(Role.DONOR));
    }

    @Test
    @DisplayName("US-006: Login with a wrong password is rejected without issuing a token")
    void testLogin_WrongPassword_ThrowsInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest("asha@example.com", "WrongPassword");

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("WrongPassword", savedUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("US-006: Login with an email that doesn't exist is rejected the same way as a wrong password")
    void testLogin_UnknownEmail_ThrowsInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest("ghost@example.com", "whatever");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
