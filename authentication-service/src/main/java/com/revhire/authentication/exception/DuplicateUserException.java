package com.revhire.authentication.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException() {
        super("Email or username is already registered");
    }
}
