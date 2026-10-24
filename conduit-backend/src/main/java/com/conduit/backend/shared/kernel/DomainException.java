package com.conduit.backend.shared.kernel;

import java.util.List;
import java.util.Map;

public abstract class DomainException extends RuntimeException {

    private final Map<String, List<String>> errors;

    protected DomainException(Map<String, List<String>> errors) {
        super(errors.toString());
        this.errors = errors;
    }

    protected DomainException(String field, String message) {
        this(Map.of(field, List.of(message)));
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
