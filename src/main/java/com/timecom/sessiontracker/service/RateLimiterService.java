package com.timecom.sessiontracker.service;

import com.timecom.sessiontracker.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory rate limiter using a sliding window approach.
 * Tracks login attempts per IP address to prevent brute force attacks.
 * For production use Redis-based rate limiting for distributed systems.
 */
@Component
public class RateLimiterService {

    private final int maxAttempts;
    private final long windowMs;

    /** Map of IP → list of attempt timestamps */
    private final Map<String, Queue<Long>> attempts = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.login-attempts}") int maxAttempts,
            @Value("${app.rate-limit.window-seconds}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowSeconds * 1000;
    }

    /**
     * Check if the IP has exceeded the rate limit.
     * @throws RateLimitExceededException if limit is exceeded
     */
    public void checkRateLimit(String ipAddress) {
        Queue<Long> timestamps = attempts.computeIfAbsent(ipAddress,
                k -> new ConcurrentLinkedQueue<>());

        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // Remove expired timestamps
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        if (timestamps.size() >= maxAttempts) {
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again in " +
                    (windowMs / 1000) + " seconds.");
        }

        timestamps.add(now);
    }

    /**
     * Reset attempts for an IP (e.g., on successful login).
     */
    public void resetAttempts(String ipAddress) {
        attempts.remove(ipAddress);
    }
}
