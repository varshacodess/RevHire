package com.revhire.authentication.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email/username or password");
    }
}
