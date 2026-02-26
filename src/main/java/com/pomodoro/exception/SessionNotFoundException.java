package com.pomodoro.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String id) {
        super("Session not found: " + id);
    }
}

