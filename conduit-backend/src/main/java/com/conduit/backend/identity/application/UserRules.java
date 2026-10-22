package com.conduit.backend.identity.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import com.conduit.backend.shared.validation.FieldErrors;
import com.conduit.backend.shared.validation.Text;

final class UserRules {

	static final int USERNAME_MIN = 3;
	static final int USERNAME_MAX = 30;
	static final int EMAIL_MAX = 254;
	static final int PASSWORD_MIN = 8;
	static final int PASSWORD_MAX = 72;
	static final int BIO_MAX = 300;
	static final int IMAGE_MAX = 2048;

	private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_-]+");
	private static final Pattern EMAIL = Pattern.compile(
			"[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
					+ "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+");

	private UserRules() {
	}

	static String username(Object raw, FieldErrors errors) {
		String value = Text.required(raw, "username", errors);
		if (value == null) {
			return null;
		}
		boolean ok = Text.min(value, USERNAME_MIN, "username", errors);
		ok &= Text.max(value, USERNAME_MAX, "username", errors);
		if (!USERNAME.matcher(value).matches()) {
			errors.add("username", Text.INVALID);
			ok = false;
		}
		return ok ? value : null;
	}

	static String email(Object raw, FieldErrors errors) {
		String value = Text.required(raw, "email", errors);
		if (value == null) {
			return null;
		}
		boolean ok = Text.max(value, EMAIL_MAX, "email", errors);
		if (!EMAIL.matcher(value).matches()) {
			errors.add("email", Text.INVALID);
			ok = false;
		}
		return ok ? value.toLowerCase(Locale.ROOT) : null;
	}

	static String password(Object raw, FieldErrors errors) {
		String value = rawPassword(raw, "password", errors);
		if (value == null) {
			return null;
		}
		boolean ok = Text.min(value, PASSWORD_MIN, "password", errors);
		ok &= Text.max(value, PASSWORD_MAX, "password", errors);
		if (ok && value.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX) {
			errors.add("password", Text.tooLong(PASSWORD_MAX));
			ok = false;
		}
		return ok ? value : null;
	}

	static String rawPassword(Object raw, String field, FieldErrors errors) {
		if (raw == null) {
			errors.add(field, Text.BLANK);
			return null;
		}
		if (!(raw instanceof String value)) {
			errors.add(field, Text.INVALID);
			return null;
		}
		if (value.isBlank()) {
			errors.add(field, Text.BLANK);
			return null;
		}
		return value;
	}

	static String bio(Object raw, FieldErrors errors) {
		String value = Text.optional(raw, "bio", errors);
		if (value != null) {
			Text.max(value, BIO_MAX, "bio", errors);
		}
		return value;
	}

	static String image(Object raw, FieldErrors errors) {
		String value = Text.optional(raw, "image", errors);
		if (value == null) {
			return null;
		}
		if (value.length() > IMAGE_MAX) {
			errors.add("image", Text.tooLong(IMAGE_MAX));
		}
		else if (!isHttpsUrl(value)) {
			errors.add("image", Text.INVALID);
		}
		return value;
	}

	static boolean isHttpsUrl(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isWhitespace(value.charAt(i))) {
				return false;
			}
		}
		try {
			URI uri = new URI(value);
			return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isBlank();
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}
}
