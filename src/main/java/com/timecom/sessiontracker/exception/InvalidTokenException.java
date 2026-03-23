package com.timecom.sessiontracker.exception;

/**
 * Thrown when a JWT token is invalid, malformed, or tampered with.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
