package com.realtimecollab.service;

import com.realtimecollab.dto.auth.AuthRequest;
import com.realtimecollab.dto.auth.AuthResponse;
import com.realtimecollab.dto.auth.RegisterRequest;
import com.realtimecollab.entity.User;
import com.realtimecollab.exception.BadRequestException;
import com.realtimecollab.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerNormalizesEmailHashesPasswordAndReturnsToken() {
        RegisterRequest request = registerRequest("Shankar", "  Shankar@Example.COM  ", "secret123");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("shankar@example.com");
        assertThat(response.getName()).isEqualTo("Shankar");

        User savedUser = userRepository.findByEmail("shankar@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", savedUser.getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCaseAndSpacing() {
        authService.register(registerRequest("Shankar", "shankar@example.com", "secret123"));

        assertThatThrownBy(() ->
                authService.register(registerRequest("Another User", "  SHANKAR@example.com  ", "secret123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An account already exists for this email.");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        authService.register(registerRequest("Shankar", "shankar@example.com", "secret123"));
        AuthRequest request = authRequest("SHANKAR@example.com", "secret123");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("shankar@example.com");
    }

    @Test
    void loginRejectsInvalidPassword() {
        authService.register(registerRequest("Shankar", "shankar@example.com", "secret123"));

        assertThatThrownBy(() -> authService.login(authRequest("shankar@example.com", "wrong-password")))
                .isInstanceOf(RuntimeException.class);
    }

    private RegisterRequest registerRequest(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private AuthRequest authRequest(String email, String password) {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
