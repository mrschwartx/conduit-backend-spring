package com.conduit.backend.shared.kernel;

public class InvalidInputException extends DomainException {

    public InvalidInputException(String field, String message) {
        super(field, message);
    }
}
