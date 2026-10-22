package com.conduit.backend.shared.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.conduit.backend.shared.errors.ApiException;

/** Collects every failing field so that a single 422 can report all of them. */
public final class FieldErrors {

	private final Map<String, List<String>> errors = new LinkedHashMap<>();

	public FieldErrors add(String field, String message) {
		List<String> messages = errors.computeIfAbsent(field, k -> new ArrayList<>());
		if (!messages.contains(message)) {
			messages.add(message);
		}
		return this;
	}

	public boolean isEmpty() {
		return errors.isEmpty();
	}

	public Map<String, List<String>> asMap() {
		return errors;
	}

	public void throwUnprocessableIfAny() {
		if (!errors.isEmpty()) {
			throw new ApiException(ApiException.UNPROCESSABLE, errors);
		}
	}

	public void throwConflictIfAny() {
		if (!errors.isEmpty()) {
			throw new ApiException(ApiException.CONFLICT, errors);
		}
	}
}
