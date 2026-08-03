package com.globallogic.bloodbridge.auth.service;

import com.globallogic.bloodbridge.auth.dto.AuthResponse;
import com.globallogic.bloodbridge.auth.dto.LoginRequest;
import com.globallogic.bloodbridge.auth.dto.RegisterRequest;
import com.globallogic.bloodbridge.auth.dto.UserResponse;
import com.globallogic.bloodbridge.auth.exception.DuplicateUserException;
import com.globallogic.bloodbridge.auth.exception.InvalidCredentialsException;
import com.globallogic.bloodbridge.auth.exception.UserNotFoundException;
import com.globallogic.bloodbridge.auth.model.User;
import com.globallogic.bloodbridge.auth.repository.UserRepository;
import com.globallogic.bloodbridge.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException(request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user id={} role={}", saved.getUserId(), saved.getRole());

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User id={} logged in", user.getUserId());
        return buildAuthResponse(user);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .build();
    }
}
