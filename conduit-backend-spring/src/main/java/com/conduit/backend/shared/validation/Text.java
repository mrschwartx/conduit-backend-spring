package com.conduit.backend.shared.validation;

/** String validation helpers. Lengths count Unicode code points, not UTF-16 units or bytes. */
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

	/**
	 * Reads a required string. Returns the trimmed value, or {@code null} after
	 * recording an error (null/absent/blank -> "can't be blank", non-string -> "is invalid").
	 */
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

	/**
	 * Reads an optional string where null or blank means "clear". Returns the trimmed
	 * value or {@code null}; a non-string records "is invalid".
	 */
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

	/** Records a message and returns false when {@code value} is shorter than {@code min}. */
	public static boolean min(String value, int min, String field, FieldErrors errors) {
		if (length(value) < min) {
			errors.add(field, tooShort(min));
			return false;
		}
		return true;
	}

	/** Records a message and returns false when {@code value} is longer than {@code max}. */
	public static boolean max(String value, int max, String field, FieldErrors errors) {
		if (length(value) > max) {
			errors.add(field, tooLong(max));
			return false;
		}
		return true;
	}
}
