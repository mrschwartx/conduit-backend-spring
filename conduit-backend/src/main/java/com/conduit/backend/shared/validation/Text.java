package com.conduit.backend.shared.validation;

public final class Text {

	public static final String BLANK = "can't be blank";
	public static final String INVALID = "is invalid";

	private Text() {
	}

	public static int length(String value) {
		return value.codePointCount(0, value.length());
	}

	public static String tooShort(int min) {
		return "is too short (minimum is " + min + " characters)";
	}

	public static String tooLong(int max) {
		return "is too long (maximum is " + max + " characters)";
	}

	public static String required(Object raw, String field, FieldErrors errors) {
		if (raw == null) {
			errors.add(field, BLANK);
			return null;
		}
		if (!(raw instanceof String s)) {
			errors.add(field, INVALID);
			return null;
		}
		String value = s.strip();
		if (value.isEmpty()) {
			errors.add(field, BLANK);
			return null;
		}
		return value;
	}

	public static String optional(Object raw, String field, FieldErrors errors) {
		if (raw == null) {
			return null;
		}
		if (!(raw instanceof String s)) {
			errors.add(field, INVALID);
			return null;
		}
		String value = s.strip();
		return value.isEmpty() ? null : value;
	}

	public static boolean min(String value, int min, String field, FieldErrors errors) {
		if (length(value) < min) {
			errors.add(field, tooShort(min));
			return false;
		}
		return true;
	}

	public static boolean max(String value, int max, String field, FieldErrors errors) {
		if (length(value) > max) {
			errors.add(field, tooLong(max));
			return false;
		}
		return true;
	}
}
