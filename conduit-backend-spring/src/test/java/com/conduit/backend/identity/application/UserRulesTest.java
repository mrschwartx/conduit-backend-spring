package com.conduit.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.conduit.backend.shared.validation.FieldErrors;

class UserRulesTest {

	private final FieldErrors errors = new FieldErrors();

	@Test
	void usernameIsTrimmedAndAccepted() {
		assertThat(UserRules.username("  Alice_01-x  ", errors)).isEqualTo("Alice_01-x");
		assertThat(errors.isEmpty()).isTrue();
	}

	@Test
	void usernameRejectsBlankShortLongAndBadCharacters() {
		assertThat(UserRules.username("   ", errors)).isNull();
		assertThat(errors.asMap().get("username")).containsExactly("can't be blank");

		FieldErrors e2 = new FieldErrors();
		assertThat(UserRules.username("ab", e2)).isNull();
		assertThat(e2.asMap().get("username")).containsExactly("is too short (minimum is 3 characters)");

		FieldErrors e3 = new FieldErrors();
		assertThat(UserRules.username("a".repeat(31), e3)).isNull();
		assertThat(e3.asMap().get("username")).containsExactly("is too long (maximum is 30 characters)");

		FieldErrors e4 = new FieldErrors();
		assertThat(UserRules.username("bad name!", e4)).isNull();
		assertThat(e4.asMap().get("username")).containsExactly("is invalid");
	}

	@Test
	void usernameLengthCountsCodePointsNotBytes() {
		// three characters, each outside the BMP (2 UTF-16 units, 4 UTF-8 bytes) -> only fails the charset rule
		FieldErrors e = new FieldErrors();
		UserRules.username("😀😀😀", e);
		assertThat(e.asMap().get("username")).containsExactly("is invalid");
	}

	@Test
	void usernameRejectsNonStrings() {
		assertThat(UserRules.username(123, errors)).isNull();
		assertThat(errors.asMap().get("username")).containsExactly("is invalid");
	}

	@Test
	void emailIsLowercasedAndValidated() {
		assertThat(UserRules.email("  Jane.Doe@Example.COM ", errors)).isEqualTo("jane.doe@example.com");
		assertThat(errors.isEmpty()).isTrue();

		for (String bad : new String[] { "nope", "a@b", "a b@c.com", "@c.com", "a@@c.com", "a@c..com" }) {
			FieldErrors e = new FieldErrors();
			assertThat(UserRules.email(bad, e)).as(bad).isNull();
			assertThat(e.asMap().get("email")).as(bad).containsExactly("is invalid");
		}
	}

	@Test
	void passwordRules() {
		assertThat(UserRules.password("  password with spaces  ", errors)).isEqualTo("  password with spaces  ");

		FieldErrors e1 = new FieldErrors();
		assertThat(UserRules.password("short", e1)).isNull();
		assertThat(e1.asMap().get("password")).containsExactly("is too short (minimum is 8 characters)");

		FieldErrors e2 = new FieldErrors();
		assertThat(UserRules.password("x".repeat(73), e2)).isNull();
		assertThat(e2.asMap().get("password")).containsExactly("is too long (maximum is 72 characters)");

		// 40 characters but 80 bytes: bcrypt would reject it, so it is refused up front
		FieldErrors e3 = new FieldErrors();
		assertThat(UserRules.password("é".repeat(37).concat("€€€"), e3)).isNull();
		assertThat(e3.asMap().get("password")).containsExactly("is too long (maximum is 72 characters)");

		FieldErrors e4 = new FieldErrors();
		assertThat(UserRules.password(null, e4)).isNull();
		assertThat(e4.asMap().get("password")).containsExactly("can't be blank");
	}

	@Test
	void bioAndImageClearOnNullOrBlank() {
		assertThat(UserRules.bio(null, errors)).isNull();
		assertThat(UserRules.bio("   ", errors)).isNull();
		assertThat(UserRules.image(null, errors)).isNull();
		assertThat(UserRules.image("", errors)).isNull();
		assertThat(errors.isEmpty()).isTrue();
	}

	@Test
	void bioMaximumIs300() {
		assertThat(UserRules.bio("b".repeat(300), errors)).hasSize(300);
		assertThat(errors.isEmpty()).isTrue();
		UserRules.bio("b".repeat(301), errors);
		assertThat(errors.asMap().get("bio")).containsExactly("is too long (maximum is 300 characters)");
	}

	@Test
	void imageMustBeHttpsUrl() {
		assertThat(UserRules.image("https://example.com/a.png", errors)).isEqualTo("https://example.com/a.png");
		assertThat(errors.isEmpty()).isTrue();

		for (String bad : new String[] { "http://example.com/a.png", "ftp://x.com/a", "example.com/a.png",
				"https://", "https://exa mple.com", "javascript:alert(1)" }) {
			FieldErrors e = new FieldErrors();
			UserRules.image(bad, e);
			assertThat(e.asMap().get("image")).as(bad).containsExactly("is invalid");
		}
	}
}
