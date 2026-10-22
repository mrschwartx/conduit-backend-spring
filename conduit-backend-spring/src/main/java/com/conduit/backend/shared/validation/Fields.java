package com.conduit.backend.shared.validation;

import java.util.LinkedHashMap;
import java.util.Map;

import com.conduit.backend.shared.errors.ApiException;

/**
 * The inner object of a request envelope ({"user": {...}}). Backed by a plain map
 * so that "key absent" and "key present with null" stay distinguishable, which
 * partial updates need.
 */
public final class Fields {

	private final Map<String, Object> values;

	private Fields(Map<String, Object> values) {
		this.values = values;
	}

	/** Extracts {@code body[key]}; a missing or non-object envelope is a 422. */
	public static Fields envelope(Map<String, Object> body, String key) {
		Object inner = body == null ? null : body.get(key);
		if (inner == null) {
			throw ApiException.of(ApiException.UNPROCESSABLE, key, Text.BLANK);
		}
		if (!(inner instanceof Map<?, ?> map)) {
			throw ApiException.of(ApiException.UNPROCESSABLE, key, Text.INVALID);
		}
		Map<String, Object> copy = new LinkedHashMap<>();
		map.forEach((k, v) -> copy.put(String.valueOf(k), v));
		return new Fields(copy);
	}

	public static Fields of(Map<String, Object> values) {
		return new Fields(new LinkedHashMap<>(values));
	}

	/** True when the key is present, even if its value is null. */
	public boolean has(String key) {
		return values.containsKey(key);
	}

	public Object get(String key) {
		return values.get(key);
	}
}
