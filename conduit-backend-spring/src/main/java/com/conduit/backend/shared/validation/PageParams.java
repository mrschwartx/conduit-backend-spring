package com.conduit.backend.shared.validation;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** Validated offset/limit query parameters. */
public record PageParams(long offset, int limit) {

	public static final int DEFAULT_LIMIT = 20;
	public static final int MAX_LIMIT = 50;

	private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
	private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

	/** Parses raw query values; any problem is reported together as a 422. */
	public static PageParams parse(String offsetRaw, String limitRaw) {
		FieldErrors errors = new FieldErrors();
		long offset = 0;
		int limit = DEFAULT_LIMIT;

		if (offsetRaw != null) {
			BigInteger value = integer(offsetRaw);
			if (value == null) {
				errors.add("offset", "must be an integer");
			}
			else if (value.signum() < 0) {
				errors.add("offset", "must be greater than or equal to 0");
			}
			else {
				offset = value.min(LONG_MAX).longValue();
			}
		}
		if (limitRaw != null) {
			BigInteger value = integer(limitRaw);
			if (value == null) {
				errors.add("limit", "must be an integer");
			}
			else if (value.signum() <= 0) {
				errors.add("limit", "must be greater than or equal to 1");
			}
			else {
				limit = value.min(BigInteger.valueOf(MAX_LIMIT)).intValue();
			}
		}
		errors.throwUnprocessableIfAny();
		return new PageParams(offset, limit);
	}

	private static BigInteger integer(String raw) {
		String s = raw.strip();
		return INTEGER.matcher(s).matches() ? new BigInteger(s.startsWith("+") ? s.substring(1) : s) : null;
	}
}
