package com.conduit.backend.shared.web;

import java.util.List;
import java.util.Map;

public record ApiError(Map<String, List<String>> errors) {

    public static ApiError of(String field, String message) {
        return new ApiError(Map.of(field, List.of(message)));
    }
}
