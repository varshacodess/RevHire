package com.revhire.authentication.exception;

public class DuplicateCandidateException extends RuntimeException {
    public DuplicateCandidateException() {
        super("Email or username is already registered");
    }
}
