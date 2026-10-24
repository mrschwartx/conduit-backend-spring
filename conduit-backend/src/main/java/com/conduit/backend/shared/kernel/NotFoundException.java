package com.conduit.backend.shared.kernel;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource) {
        super(resource, "not found");
    }
}
