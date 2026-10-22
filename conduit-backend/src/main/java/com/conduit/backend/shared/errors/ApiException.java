package com.conduit.backend.shared.errors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiException extends RuntimeException {

	public static final int UNAUTHORIZED = 401;
	public static final int FORBIDDEN = 403;
	public static final int NOT_FOUND = 404;
	public static final int CONFLICT = 409;
	public static final int UNPROCESSABLE = 422;

	private final int status;
	private final transient Map<String, List<String>> errors;

	public ApiException(int status, Map<String, List<String>> errors) {
		super("HTTP " + status + " " + errors, null, false, false);
		this.status = status;
		this.errors = new LinkedHashMap<>(errors);
	}

	public static ApiException of(int status, String key, String message) {
		return new ApiException(status, Map.of(key, List.of(message)));
	}

	public static ApiException notFound(String resource) {
		return of(NOT_FOUND, resource, "not found");
	}

	public static ApiException forbidden(String resource) {
		return of(FORBIDDEN, resource, "forbidden");
	}

	public static ApiException unauthorized(String key, String message) {
		return of(UNAUTHORIZED, key, message);
	}

	public int status() {
		return status;
	}

	public Map<String, List<String>> errors() {
		return errors;
	}
}
