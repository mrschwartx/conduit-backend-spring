package com.conduit.backend.articles.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.conduit.backend.shared.validation.FieldErrors;
import com.conduit.backend.shared.validation.Text;

final class ArticleRules {

	static final int TITLE_MAX = 100;
	static final int DESCRIPTION_MAX = 200;
	static final int BODY_MAX = 20_000;
	static final int COMMENT_BODY_MAX = 1000;
	static final int TAG_MAX_COUNT = 10;
	static final int TAG_MAX_LENGTH = 30;

	private ArticleRules() {
	}

	static String title(Object raw, FieldErrors errors) {
		return requiredMax(raw, "title", TITLE_MAX, errors);
	}

	static String description(Object raw, FieldErrors errors) {
		return requiredMax(raw, "description", DESCRIPTION_MAX, errors);
	}

	static String body(Object raw, FieldErrors errors) {
		return requiredMax(raw, "body", BODY_MAX, errors);
	}

	static String commentBody(Object raw, FieldErrors errors) {
		return requiredMax(raw, "body", COMMENT_BODY_MAX, errors);
	}

	private static String requiredMax(Object raw, String field, int max, FieldErrors errors) {
		String value = Text.required(raw, field, errors);
		if (value == null) {
			return null;
		}
		return Text.max(value, max, field, errors) ? value : null;
	}

	static List<String> tags(Object raw, FieldErrors errors) {
		if (raw == null) {
			return List.of();
		}
		if (!(raw instanceof List<?> list)) {
			errors.add("tagList", Text.INVALID);
			return List.of();
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		boolean invalidTag = false;
		for (Object item : list) {
			if (!(item instanceof String s)) {
				invalidTag = true;
				continue;
			}
			String tag = s.strip();
			int length = Text.length(tag);
			if (length < 1 || length > TAG_MAX_LENGTH || hasWhitespaceOrComma(tag)) {
				invalidTag = true;
				continue;
			}
			unique.add(tag);
		}
		if (invalidTag) {
			errors.add("tagList", "contains an invalid tag (1-30 characters, no whitespace or commas)");
		}
		if (unique.size() > TAG_MAX_COUNT) {
			errors.add("tagList", "is too long (maximum is " + TAG_MAX_COUNT + " tags)");
		}
		return new ArrayList<>(unique);
	}

	private static boolean hasWhitespaceOrComma(String value) {
		return value.codePoints().anyMatch(cp -> cp == ',' || Character.isWhitespace(cp) || Character.isSpaceChar(cp));
	}
}
