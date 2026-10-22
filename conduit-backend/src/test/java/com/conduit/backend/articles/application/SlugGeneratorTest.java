package com.conduit.backend.articles.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

	@Test
	void lowercasesAndReplacesRunsOfSymbolsWithOneDash() {
		assertThat(SlugGenerator.slugify("Hello World!")).isEqualTo("hello-world");
		assertThat(SlugGenerator.slugify("  Hello,   World -- again  ")).isEqualTo("hello-world-again");
		assertThat(SlugGenerator.slugify("C++ & Java 25")).isEqualTo("c-java-25");
	}

	@Test
	void stripsDiacriticsAndNormalisesCompatibilityCharacters() {
		assertThat(SlugGenerator.slugify("Crème Brûlée & Café")).isEqualTo("creme-brulee-cafe");
		assertThat(SlugGenerator.slugify("Ångström")).isEqualTo("angstrom");
		assertThat(SlugGenerator.slugify("ﬁne")).isEqualTo("fine");
		assertThat(SlugGenerator.slugify("İstanbul")).isEqualTo("istanbul");
	}

	@Test
	void fallsBackToArticleWhenNothingIsLeft() {
		assertThat(SlugGenerator.slugify("!!!")).isEqualTo("article");
		assertThat(SlugGenerator.slugify("日本語")).isEqualTo("article");
		assertThat(SlugGenerator.slugify("   ")).isEqualTo("article");
	}

	@Test
	void isCappedAtEightyCharactersWithoutTrailingDash() {
		assertThat(SlugGenerator.slugify("a".repeat(200))).hasSize(80);
		String slug = SlugGenerator.slugify("word ".repeat(40));
		assertThat(slug.length()).isLessThanOrEqualTo(80);
		assertThat(slug).doesNotStartWith("-").doesNotEndWith("-");
		assertThat(SlugGenerator.slugify("a".repeat(79) + " b")).isEqualTo("a".repeat(79));
	}

	@Test
	void firstFreeReturnsTheBaseWhenAvailable() {
		assertThat(SlugGenerator.firstFree("hello", Set.of())).isEqualTo("hello");
		assertThat(SlugGenerator.firstFree("hello", Set.of("hello-2"))).isEqualTo("hello");
	}

	@Test
	void firstFreeAppendsIncreasingSuffixes() {
		assertThat(SlugGenerator.firstFree("hello", Set.of("hello"))).isEqualTo("hello-2");
		assertThat(SlugGenerator.firstFree("hello", Set.of("hello", "hello-2", "hello-3"))).isEqualTo("hello-4");
		assertThat(SlugGenerator.firstFree("hello", Set.of("hello", "hello-3"))).isEqualTo("hello-2");
	}

	@Test
	void feedIsReserved() {
		assertThat(SlugGenerator.firstFree("feed", Set.of())).isEqualTo("feed-2");
		assertThat(SlugGenerator.firstFree("feed", Set.of("feed-2"))).isEqualTo("feed-3");
		assertThat(SlugGenerator.firstFree("feeds", Set.of())).isEqualTo("feeds");
	}
}
