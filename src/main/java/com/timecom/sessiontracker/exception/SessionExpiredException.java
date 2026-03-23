package com.timecom.sessiontracker.exception;

/**
 * Thrown when a session has expired or is no longer valid.
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
