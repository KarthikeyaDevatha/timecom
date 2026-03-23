package com.timecom.sessiontracker.exception;

/**
 * Thrown when a user is not found in the database.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
