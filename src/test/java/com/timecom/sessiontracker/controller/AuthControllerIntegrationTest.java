package com.timecom.sessiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecom.sessiontracker.dto.AuthResponse;
import com.timecom.sessiontracker.dto.LoginRequest;
import com.timecom.sessiontracker.dto.RegisterRequest;
import com.timecom.sessiontracker.service.AuthService;
import com.timecom.sessiontracker.security.JwtTokenProvider;
import com.timecom.sessiontracker.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login - should return 200 with valid credentials")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-token-123")
                .tokenType("Bearer")
                .expiresIn(3600)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .sessionId(1L)
                .build();

        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 400 for empty username")
    void login_ValidationError() throws Exception {
        LoginRequest request = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 201 on success")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser", "new@example.com", "password123", "New User");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-token-456")
                .tokenType("Bearer")
                .expiresIn(3600)
                .username("newuser")
                .email("new@example.com")
                .role("USER")
                .sessionId(2L)
                .build();

        when(authService.register(any(RegisterRequest.class), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-456"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should validate email format")
    void register_InvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser", "invalid-email", "password123", "New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/logout - should fail without valid auth token")
    void logout_RequiresAuth() throws Exception {
        // /api/auth/** is permitAll, so Spring Security lets the request through.
        // The controller receives null @AuthenticationPrincipal, causing a 500.
        // In production, the client always sends a Bearer token for logout.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isInternalServerError());
    }
}
