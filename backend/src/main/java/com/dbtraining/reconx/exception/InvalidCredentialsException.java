package com.dbtraining.reconx.exception;

/**
 * Thrown when login credentials are invalid.
 */
public class InvalidCredentialsException extends ReconException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
