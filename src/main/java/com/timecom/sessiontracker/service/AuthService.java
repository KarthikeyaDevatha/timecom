package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.dto.*;
import com.timecom.sessiontracker.entity.Session;
import com.timecom.sessiontracker.entity.User;
import com.timecom.sessiontracker.entity.enums.ActionType;
import com.timecom.sessiontracker.exception.UserNotFoundException;
import com.timecom.sessiontracker.repository.UserRepository;
import com.timecom.sessiontracker.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service handling user authentication: login, registration, and logout.
 * Coordinates JWT token issuance, session creation, and activity tracking.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final TrackingService trackingService;
    private final RateLimiterService rateLimiterService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SessionService sessionService,
                       TrackingService trackingService,
                       RateLimiterService rateLimiterService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.trackingService = trackingService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Authenticate a user and create a new session.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Rate limit check
        rateLimiterService.checkRateLimit(ipAddress);

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        // Generate JWT
        String token = jwtTokenProvider.generateToken(authentication);

        // Get user (support login by username or email)
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + request.getUsername()));

        // Create session
        Session session = sessionService.createSession(user, token, ipAddress, userAgent);

        // Track login event
        trackingService.trackEventAsync(
                new TrackEventRequest(ActionType.LOGIN, "/auth/login", null,
                        Map.of("ipAddress", ipAddress, "deviceType",
                                session.getDeviceType().name())),
                user, session, ipAddress);

        // Clear rate limit on successful login
        rateLimiterService.resetAttempts(ipAddress);

        log.info("User {} logged in from IP {}", user.getUsername(), ipAddress);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .sessionId(session.getId())
                .build();
    }

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        // Check for existing user
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getUsername());

        // Auto-login after registration
        String token = jwtTokenProvider.generateToken(user.getUsername());
        Session session = sessionService.createSession(user, token, ipAddress, userAgent);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .sessionId(session.getId())
                .build();
    }

    /**
     * Logout: terminate session and blacklist token.
     */
    @Transactional
    public void logout(String token, String username) {
        // Terminate the session
        sessionService.terminateSession(token);

        // Blacklist the JWT token
        jwtTokenProvider.blacklistToken(token);

        log.info("User {} logged out", username);
    }
}
