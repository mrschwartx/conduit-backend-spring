package com.conduit.backend.shared.errors;

import java.util.List;
import java.util.Map;

public record ErrorResponse(Map<String, List<String>> errors) {
}
