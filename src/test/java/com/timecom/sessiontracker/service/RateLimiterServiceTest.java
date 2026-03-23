package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimiterService.
 */
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // 3 attempts per 60 seconds
        rateLimiterService = new RateLimiterService(3, 60);
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void checkRateLimit_ShouldAllowWithinLimit() {
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit("192.168.1.1"));
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit("192.168.1.1"));
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit("192.168.1.1"));
    }

    @Test
    @DisplayName("Should throw when rate limit exceeded")
    void checkRateLimit_ShouldThrowWhenExceeded() {
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiterService.checkRateLimit("192.168.1.1"));
    }

    @Test
    @DisplayName("Should track IPs independently")
    void checkRateLimit_ShouldTrackIPsIndependently() {
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");

        // Different IP should still be allowed
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit("192.168.1.2"));
    }

    @Test
    @DisplayName("Should reset attempts on successful login")
    void resetAttempts_ShouldClearHistory() {
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");
        rateLimiterService.checkRateLimit("192.168.1.1");

        rateLimiterService.resetAttempts("192.168.1.1");

        // Should be allowed again
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit("192.168.1.1"));
    }
}
