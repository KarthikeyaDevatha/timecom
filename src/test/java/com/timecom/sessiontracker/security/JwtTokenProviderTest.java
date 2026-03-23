package com.timecom.sessiontracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    // Base64-encoded 512-bit key
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" +
            "337336763979244226452948404D6351655468576D5A7134743777217A25432A46";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 3600000, 900000);
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void generateToken_ShouldReturnNonNullToken() {
        String token = provider.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should extract username from token")
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        String token = provider.generateToken("testuser");
        String username = provider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should validate a valid token")
    void validateToken_ShouldReturnTrueForValidToken() {
        String token = provider.generateToken("testuser");
        assertTrue(provider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject an invalid token")
    void validateToken_ShouldReturnFalseForInvalidToken() {
        assertFalse(provider.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("Should reject an empty token")
    void validateToken_ShouldReturnFalseForEmptyToken() {
        assertFalse(provider.validateToken(""));
    }

    @Test
    @DisplayName("Should reject a blacklisted token")
    void validateToken_ShouldReturnFalseForBlacklistedToken() {
        String token = provider.generateToken("testuser");
        assertTrue(provider.validateToken(token));

        provider.blacklistToken(token);
        assertFalse(provider.validateToken(token));
    }

    @Test
    @DisplayName("Should detect blacklisted tokens")
    void isTokenBlacklisted_ShouldReturnCorrectStatus() {
        String token = provider.generateToken("testuser");
        assertFalse(provider.isTokenBlacklisted(token));

        provider.blacklistToken(token);
        assertTrue(provider.isTokenBlacklisted(token));
    }

    @Test
    @DisplayName("Token should not be refreshable when just created")
    void isTokenRefreshable_ShouldReturnFalseForNewToken() {
        String token = provider.generateToken("testuser");
        // Token was just created, it has ~60 min to expiry, not within 15 min window
        assertFalse(provider.isTokenRefreshable(token));
    }

    @Test
    @DisplayName("Should handle expired token gracefully")
    void validateToken_ShouldHandleExpiredToken() {
        // Create provider with 0ms expiry
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 0, 0);
        String token = shortLivedProvider.generateToken("testuser");

        // Token expires immediately
        assertFalse(shortLivedProvider.validateToken(token));
    }
}
