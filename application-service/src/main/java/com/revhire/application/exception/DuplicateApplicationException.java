package com.revhire.application.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(String message) {
        super(message);
    }

    public DuplicateApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
