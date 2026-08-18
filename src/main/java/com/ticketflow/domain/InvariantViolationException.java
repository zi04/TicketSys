package com.ticketflow.domain;

public class InvariantViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvariantViolationException(String message) {
        super(message);
    }
}
