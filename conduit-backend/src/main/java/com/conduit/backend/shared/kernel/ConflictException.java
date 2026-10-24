package com.conduit.backend.shared.kernel;

import java.util.List;
import java.util.Map;

public class ConflictException extends DomainException {

    public ConflictException(Map<String, List<String>> errors) {
        super(errors);
    }

    public ConflictException(String field, String message) {
        super(field, message);
    }
}
