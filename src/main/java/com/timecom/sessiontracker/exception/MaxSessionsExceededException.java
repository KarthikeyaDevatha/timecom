package com.timecom.sessiontracker.exception;

/**
 * Thrown when maximum session limit per user is reached.
 */
public class MaxSessionsExceededException extends RuntimeException {

    public MaxSessionsExceededException(String message) {
        super(message);
    }
}
