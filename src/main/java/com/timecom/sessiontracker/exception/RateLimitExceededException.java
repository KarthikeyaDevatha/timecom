package com.timecom.sessiontracker.exception;

/**
 * Thrown when rate limit is exceeded (e.g., too many login attempts).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
