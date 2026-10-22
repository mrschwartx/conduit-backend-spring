package com.conduit.backend.shared.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** UTC ISO-8601 timestamps with fixed millisecond precision, e.g. 2026-10-21T10:15:30.123Z. */
public final class Timestamps {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.withZone(ZoneOffset.UTC);

	private Timestamps() {
	}

	public static String format(Instant instant) {
		return FORMAT.format(instant);
	}
}
