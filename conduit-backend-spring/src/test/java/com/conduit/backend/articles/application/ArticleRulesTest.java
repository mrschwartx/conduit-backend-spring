package com.conduit.backend.articles.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.conduit.backend.shared.validation.FieldErrors;

class ArticleRulesTest {

	private final FieldErrors errors = new FieldErrors();

	@Test
	void tagsAreTrimmedDeduplicatedAndKeepFirstSeenOrder() {
		List<String> tags = ArticleRules.tags(List.of(" java ", "spring", "java", "boot", "spring"), errors);
		assertThat(tags).containsExactly("java", "spring", "boot");
		assertThat(errors.isEmpty()).isTrue();
	}

	@Test
	void tagsDeduplicationIsCaseSensitive() {
		assertThat(ArticleRules.tags(List.of("Java", "java"), errors)).containsExactly("Java", "java");
	}

	@Test
	void nullMeansNoTags() {
		assertThat(ArticleRules.tags(null, errors)).isEmpty();
		assertThat(errors.isEmpty()).isTrue();
	}

	@Test
	void tenTagsAreFineElevenAreNot() {
		List<String> ten = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ten.add("t" + i);
		}
		assertThat(ArticleRules.tags(ten, errors)).hasSize(10);
		assertThat(errors.isEmpty()).isTrue();

		ten.add("t10");
		ArticleRules.tags(ten, errors);
		assertThat(errors.asMap().get("tagList")).containsExactly("is too long (maximum is 10 tags)");
	}

	@Test
	void duplicatesDoNotCountTowardsTheLimit() {
		List<String> tags = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			tags.add("t" + (i % 10));
		}
		assertThat(ArticleRules.tags(tags, errors)).hasSize(10);
		assertThat(errors.isEmpty()).isTrue();
	}

	@Test
	void invalidTagsAreRejected() {
		for (Object bad : new Object[] { "has space", "has,comma", "", "   ", "x".repeat(31), 42, "tab\there" }) {
			FieldErrors e = new FieldErrors();
			ArticleRules.tags(List.of("ok", bad), e);
			assertThat(e.asMap()).as(String.valueOf(bad)).containsKey("tagList");
		}
	}

	@Test
	void tagLengthCountsCodePoints() {
		assertThat(ArticleRules.tags(List.of("😀".repeat(30)), errors)).hasSize(1);
		assertThat(errors.isEmpty()).isTrue();
		ArticleRules.tags(List.of("😀".repeat(31)), errors);
		assertThat(errors.isEmpty()).isFalse();
	}

	@Test
	void tagListMustBeAnArray() {
		ArticleRules.tags("java", errors);
		assertThat(errors.asMap().get("tagList")).containsExactly("is invalid");
	}

	@Test
	void titleDescriptionAndBodyLimits() {
		assertThat(ArticleRules.title("t".repeat(100), errors)).hasSize(100);
		assertThat(ArticleRules.description("d".repeat(200), errors)).hasSize(200);
		assertThat(ArticleRules.body("b".repeat(20_000), errors)).hasSize(20_000);
		assertThat(errors.isEmpty()).isTrue();

		ArticleRules.title("t".repeat(101), errors);
		ArticleRules.description("d".repeat(201), errors);
		ArticleRules.body("b".repeat(20_001), errors);
		assertThat(errors.asMap().get("title")).containsExactly("is too long (maximum is 100 characters)");
		assertThat(errors.asMap().get("description")).containsExactly("is too long (maximum is 200 characters)");
		assertThat(errors.asMap().get("body")).containsExactly("is too long (maximum is 20000 characters)");
	}

	@Test
	void blankRequiredFieldsAreRejected() {
		assertThat(ArticleRules.title("   ", errors)).isNull();
		assertThat(ArticleRules.description(null, errors)).isNull();
		assertThat(ArticleRules.body(7, errors)).isNull();
		assertThat(errors.asMap().get("title")).containsExactly("can't be blank");
		assertThat(errors.asMap().get("description")).containsExactly("can't be blank");
		assertThat(errors.asMap().get("body")).containsExactly("is invalid");
	}

	@Test
	void commentBodyLimitIs1000() {
		assertThat(ArticleRules.commentBody("c".repeat(1000), errors)).hasSize(1000);
		assertThat(errors.isEmpty()).isTrue();
		ArticleRules.commentBody("c".repeat(1001), errors);
		assertThat(errors.asMap().get("body")).containsExactly("is too long (maximum is 1000 characters)");
	}
}
