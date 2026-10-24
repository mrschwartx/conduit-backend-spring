package com.conduit.backend.shared.kernel;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String field, String message) {
        super(field, message);
    }
}
