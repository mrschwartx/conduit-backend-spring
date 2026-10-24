package com.conduit.backend.shared.kernel;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String resource) {
        super(resource, "forbidden");
    }
}
