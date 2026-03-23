package com.timecom.sessiontracker.controller;

import com.timecom.sessiontracker.dto.SessionResponse;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.exception.UserNotFoundException;
import com.timecom.sessiontracker.repository.UserRepository;
import com.timecom.sessiontracker.security.JwtTokenProvider;
import com.timecom.sessiontracker.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Session management REST controller.
 * Provides endpoints for session info, listing, refresh, and termination.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SessionController(SessionService sessionService,
                             UserRepository userRepository,
                             JwtTokenProvider jwtTokenProvider) {
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * GET /api/sessions/current
     * Get current session information.
     */
    @GetMapping("/current")
    public ResponseEntity<SessionResponse> getCurrentSession(
            HttpServletRequest request) {

        String token = extractToken(request);
        SessionResponse session = sessionService.getSessionByToken(token);
        return ResponseEntity.ok(session);
    }

    /**
     * GET /api/sessions
     * List all active sessions for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getUserSessions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails.getUsername());
        List<SessionResponse> sessions = sessionService.getUserSessions(user.getId());
        return ResponseEntity.ok(sessions);
    }

    /**
     * POST /api/sessions/refresh
     * Refresh the current session (extend expiry, issue new token).
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshSession(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        String oldToken = extractToken(request);
        String newToken = jwtTokenProvider.generateToken(userDetails.getUsername());

        sessionService.refreshSession(oldToken, newToken);

        // Blacklist old token
        jwtTokenProvider.blacklistToken(oldToken);

        return ResponseEntity.ok(Map.of(
                "token", newToken,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenProvider.getExpirationMs() / 1000,
                "message", "Session refreshed successfully"
        ));
    }

    /**
     * DELETE /api/sessions/{id}
     * Terminate a specific session (e.g., remote logout).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> terminateSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify the session belongs to the current user
        User user = getUser(userDetails.getUsername());
        List<SessionResponse> userSessions = sessionService.getUserSessions(user.getId());

        boolean ownsSession = userSessions.stream()
                .anyMatch(s -> s.getId().equals(id));

        if (!ownsSession) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You can only terminate your own sessions"));
        }

        sessionService.terminateSessionById(id);
        return ResponseEntity.ok(Map.of("message", "Session terminated successfully"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + username));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
