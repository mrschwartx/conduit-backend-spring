package com.conduit.backend.articles.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Slug rules shared by every Conduit backend. */
public final class SlugGenerator {

	public static final int MAX_LENGTH = 80;
	public static final String FALLBACK = "article";
	/** Slugs that collide with static routes (/articles/feed) and may never be issued. */
	public static final Set<String> RESERVED = Set.of("feed");

	private SlugGenerator() {
	}

	/**
	 * Unicode NFKD normalise, strip diacritics, lower-case, collapse runs of
	 * non [a-z0-9] into "-", trim "-", cap at 80 characters, fall back to "article".
	 */
	public static String slugify(String title) {
		String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
		String slug = trimDashes(normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"));
		if (slug.length() > MAX_LENGTH) {
			slug = trimDashes(slug.substring(0, MAX_LENGTH));
		}
		return slug.isEmpty() ? FALLBACK : slug;
	}

	/** The base itself if free (and not reserved), otherwise base-2, base-3, ... */
	public static String firstFree(String base, Set<String> taken) {
		if (!RESERVED.contains(base) && !taken.contains(base)) {
			return base;
		}
		for (int suffix = 2;; suffix++) {
			String candidate = base + "-" + suffix;
			if (!taken.contains(candidate)) {
				return candidate;
			}
		}
	}

	private static String trimDashes(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && value.charAt(start) == '-') {
			start++;
		}
		while (end > start && value.charAt(end - 1) == '-') {
			end--;
		}
		return value.substring(start, end);
	}
}
