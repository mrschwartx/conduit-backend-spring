package com.conduit.backend.articles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.conduit.backend.support.AbstractApiTest;

class ArticlesApiTest extends AbstractApiTest {

	private static final String ARTICLES = V1 + "/articles";

	private static String article(String title, String description, String body, String tagsJson) {
		return """
				{"article":{"title":"%s","description":"%s","body":"%s","tagList":%s}}"""
				.formatted(title, description, body, tagsJson);
	}


	@Test
	void createReturns201WithTheFullArticleShape() {
		String token = register("alice");
		Resp r = post(ARTICLES, token, article("Hello World", "About hello", "Body text", "[\"java\",\"spring\"]"));

		assertThat(r.status()).isEqualTo(201);
		assertThat((String) r.json("$.article.slug")).isEqualTo("hello-world");
		assertThat((String) r.json("$.article.title")).isEqualTo("Hello World");
		assertThat((String) r.json("$.article.body")).isEqualTo("Body text");
		assertThat((List<String>) r.json("$.article.tagList")).containsExactly("java", "spring");
		assertThat((Boolean) r.json("$.article.favorited")).isFalse();
		assertThat((Integer) r.json("$.article.favoritesCount")).isZero();
		assertThat((String) r.json("$.article.author.username")).isEqualTo("alice");
		assertThat((Boolean) r.json("$.article.author.following")).isFalse();
		Map<String, Object> author = r.json("$.article.author");
		assertThat(author).containsEntry("bio", null).containsEntry("image", null);
		String iso = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
		assertThat((String) r.json("$.article.createdAt")).matches(iso);
		assertThat((String) r.json("$.article.updatedAt")).matches(iso);
	}

	@Test
	void createWithoutTagListGivesAnEmptyList() {
		String token = register("alice");
		Resp r = post(ARTICLES, token, "{\"article\":{\"title\":\"T\",\"description\":\"d\",\"body\":\"b\"}}");
		assertThat(r.status()).isEqualTo(201);
		assertThat((List<?>) r.json("$.article.tagList")).isEmpty();
	}

	@Test
	void createNormalisesTags() {
		String token = register("alice");
		Resp r = post(ARTICLES, token, article("T", "d", "b", "[\" a \",\"b\",\"a\"]"));
		assertThat((List<String>) r.json("$.article.tagList")).containsExactly("a", "b");
	}

	@Test
	void createValidatesEverythingAtOnce() {
		String token = register("alice");
		Resp r = post(ARTICLES, token, article("", "", "", "[\"has space\"]"));

		assertThat(r.status()).isEqualTo(422);
		assertThat((String) r.json("$.errors.title[0]")).isEqualTo("can't be blank");
		assertThat((String) r.json("$.errors.description[0]")).isEqualTo("can't be blank");
		assertThat((String) r.json("$.errors.body[0]")).isEqualTo("can't be blank");
		assertThat((Object) r.json("$.errors.tagList")).isNotNull();
	}

	@Test
	void createRequiresAuthentication() {
		assertThat(post(ARTICLES, null, article("T", "d", "b", "[]")).status()).isEqualTo(401);
	}

	@Test
	void slugCollisionsGetNumericSuffixesAndFeedIsReserved() {
		String token = register("alice");
		assertThat(createArticle(token, "Hello World")).isEqualTo("hello-world");
		assertThat(createArticle(token, "Hello World")).isEqualTo("hello-world-2");
		assertThat(createArticle(token, "hello, world!")).isEqualTo("hello-world-3");
		assertThat(createArticle(token, "Feed")).isEqualTo("feed-2");
		assertThat(createArticle(token, "!!!")).isEqualTo("article");
	}

	@Test
	void getArticleIsAvailableAnonymouslyAnd404WhenMissing() {
		String token = register("alice");
		String slug = createArticle(token, "Readable");

		Resp ok = get(ARTICLES + "/" + slug, null);
		assertThat(ok.status()).isEqualTo(200);
		assertThat((String) ok.json("$.article.body")).isEqualTo("b");

		Resp missing = get(ARTICLES + "/nope", null);
		assertThat(missing.status()).isEqualTo(404);
		assertThat((String) missing.json("$.errors.article[0]")).isEqualTo("not found");
	}

	@Test
	void getWithAnInvalidTokenIs401EvenThoughAuthIsOptional() {
		String token = register("alice");
		String slug = createArticle(token, "Readable");
		assertThat(get(ARTICLES + "/" + slug, "garbage").status()).isEqualTo(401);
	}


	@Test
	void listOmitsBodyAndIsNewestFirst() {
		String token = register("alice");
		createArticle(token, "First");
		createArticle(token, "Second");
		createArticle(token, "Third");

		Resp r = get(ARTICLES, null);
		assertThat(r.status()).isEqualTo(200);
		assertThat((Integer) r.json("$.articlesCount")).isEqualTo(3);
		assertThat((List<String>) r.json("$.articles[*].slug")).containsExactly("third", "second", "first");
		Map<String, Object> item = r.json("$.articles[0]");
		assertThat(item).doesNotContainKey("body");
		assertThat(item).containsKeys("slug", "title", "description", "tagList", "createdAt", "updatedAt",
				"favorited", "favoritesCount", "author");
	}

	@Test
	void paginationReturnsTheRequestedWindowAndTheTotalCount() {
		String token = register("alice");
		for (int i = 1; i <= 5; i++) {
			createArticle(token, "Article " + i);
		}

		Resp r = get(ARTICLES + "?offset=1&limit=2", null);
		assertThat((List<String>) r.json("$.articles[*].slug")).containsExactly("article-4", "article-3");
		assertThat((Integer) r.json("$.articlesCount")).isEqualTo(5);

		Resp beyond = get(ARTICLES + "?offset=50", null);
		assertThat(beyond.status()).isEqualTo(200);
		assertThat((List<?>) beyond.json("$.articles")).isEmpty();
		assertThat((Integer) beyond.json("$.articlesCount")).isEqualTo(5);

		assertThat(get(ARTICLES + "?limit=1000", null).status()).isEqualTo(200);
	}

	@Test
	void invalidPaginationIs422() {
		for (String query : new String[] { "limit=0", "limit=-3", "limit=abc", "offset=-1", "offset=1.5", "offset=" }) {
			Resp r = get(ARTICLES + "?" + query, null);
			assertThat(r.status()).as(query).isEqualTo(422);
		}
		Resp both = get(ARTICLES + "?offset=-1&limit=0", null);
		assertThat((Object) both.json("$.errors.offset")).isNotNull();
		assertThat((Object) both.json("$.errors.limit")).isNotNull();
	}

	@Test
	void filtersByTagAuthorAndFavoritedAndCombinesThemWithAnd() {
		String alice = register("alice");
		String bob = register("bob");
		String a1 = createArticle(alice, "Alice Java", "java", "spring");
		createArticle(alice, "Alice Go", "go");
		String b1 = createArticle(bob, "Bob Java", "java");
		post(ARTICLES + "/" + a1 + "/favorite", bob, null);
		post(ARTICLES + "/" + b1 + "/favorite", bob, null);

		assertThat((List<String>) get(ARTICLES + "?tag=java", null).json("$.articles[*].slug"))
				.containsExactly("bob-java", "alice-java");
		assertThat((List<String>) get(ARTICLES + "?author=ALICE", null).json("$.articles[*].slug"))
				.containsExactly("alice-go", "alice-java");
		assertThat((List<String>) get(ARTICLES + "?favorited=bob", null).json("$.articles[*].slug"))
				.containsExactly("bob-java", "alice-java");
		assertThat((List<String>) get(ARTICLES + "?tag=java&author=alice&favorited=bob", null)
				.json("$.articles[*].slug")).containsExactly("alice-java");
		assertThat((Integer) get(ARTICLES + "?tag=java&author=bob", null).json("$.articlesCount")).isEqualTo(1);
	}

	@Test
	void unknownAuthorOrFavoritedUserGivesAnEmptyListNotA404() {
		String alice = register("alice");
		createArticle(alice, "Something");

		for (String query : new String[] { "author=ghost", "favorited=ghost", "tag=nothing" }) {
			Resp r = get(ARTICLES + "?" + query, null);
			assertThat(r.status()).as(query).isEqualTo(200);
			assertThat((List<?>) r.json("$.articles")).as(query).isEmpty();
			assertThat((Integer) r.json("$.articlesCount")).as(query).isZero();
		}
	}

	@Test
	void listFlagsAreRelativeToTheCaller() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Liked");
		post(ARTICLES + "/" + slug + "/favorite", bob, null);
		post(V1 + "/profiles/alice/follow", bob, null);

		Resp asBob = get(ARTICLES, bob);
		assertThat((Boolean) asBob.json("$.articles[0].favorited")).isTrue();
		assertThat((Integer) asBob.json("$.articles[0].favoritesCount")).isEqualTo(1);
		assertThat((Boolean) asBob.json("$.articles[0].author.following")).isTrue();

		Resp anonymous = get(ARTICLES, null);
		assertThat((Boolean) anonymous.json("$.articles[0].favorited")).isFalse();
		assertThat((Integer) anonymous.json("$.articles[0].favoritesCount")).isEqualTo(1);
		assertThat((Boolean) anonymous.json("$.articles[0].author.following")).isFalse();
	}


	@Test
	void feedContainsOnlyArticlesFromFollowedAuthors() {
		String alice = register("alice");
		String bob = register("bob");
		String carol = register("carol");
		createArticle(bob, "From Bob");
		createArticle(carol, "From Carol");
		createArticle(alice, "From Alice");
		post(V1 + "/profiles/bob/follow", alice, null);

		Resp r = get(ARTICLES + "/feed", alice);
		assertThat(r.status()).isEqualTo(200);
		assertThat((List<String>) r.json("$.articles[*].slug")).containsExactly("from-bob");
		assertThat((Integer) r.json("$.articlesCount")).isEqualTo(1);
		Map<String, Object> item = r.json("$.articles[0]");
		assertThat(item).doesNotContainKey("body");
	}

	@Test
	void feedRequiresAuthenticationAndPaginates() {
		assertThat(get(ARTICLES + "/feed", null).status()).isEqualTo(401);

		String alice = register("alice");
		String bob = register("bob");
		post(V1 + "/profiles/bob/follow", alice, null);
		for (int i = 1; i <= 3; i++) {
			createArticle(bob, "Post " + i);
		}
		Resp page = get(ARTICLES + "/feed?limit=2&offset=1", alice);
		assertThat((List<String>) page.json("$.articles[*].slug")).containsExactly("post-2", "post-1");
		assertThat((Integer) page.json("$.articlesCount")).isEqualTo(3);
		assertThat(get(ARTICLES + "/feed?limit=0", alice).status()).isEqualTo(422);
	}


	@Test
	void partialUpdateChangesOnlyTheSentFields() {
		String token = register("alice");
		String slug = createArticle(token, "Original", "a", "b");

		Resp r = put(ARTICLES + "/" + slug, token, "{\"article\":{\"body\":\"new body\"}}");
		assertThat(r.status()).isEqualTo(200);
		assertThat((String) r.json("$.article.body")).isEqualTo("new body");
		assertThat((String) r.json("$.article.title")).isEqualTo("Original");
		assertThat((String) r.json("$.article.slug")).isEqualTo("original");
		assertThat((List<String>) r.json("$.article.tagList")).containsExactly("a", "b");

		Resp tags = put(ARTICLES + "/" + slug, token, "{\"article\":{\"tagList\":[\"z\"]}}");
		assertThat((List<String>) tags.json("$.article.tagList")).containsExactly("z");
		assertThat((String) tags.json("$.article.body")).isEqualTo("new body");
	}

	@Test
	void changingTheTitleRegeneratesTheSlugButAnUnchangedTitleKeepsIt() {
		String token = register("alice");
		createArticle(token, "Taken Title");
		String slug = createArticle(token, "Old Title");

		Resp same = put(ARTICLES + "/" + slug, token, "{\"article\":{\"title\":\"Old Title\"}}");
		assertThat((String) same.json("$.article.slug")).isEqualTo("old-title");

		Resp renamed = put(ARTICLES + "/" + slug, token, "{\"article\":{\"title\":\"Brand New\"}}");
		assertThat((String) renamed.json("$.article.slug")).isEqualTo("brand-new");
		assertThat(get(ARTICLES + "/old-title", null).status()).isEqualTo(404);
		assertThat(get(ARTICLES + "/brand-new", null).status()).isEqualTo(200);

		Resp collide = put(ARTICLES + "/brand-new", token, "{\"article\":{\"title\":\"taken title\"}}");
		assertThat((String) collide.json("$.article.slug")).isEqualTo("taken-title-2");

		Resp ownSlug = put(ARTICLES + "/taken-title-2", token, "{\"article\":{\"title\":\"TAKEN TITLE 2\"}}");
		assertThat((String) ownSlug.json("$.article.slug")).isEqualTo("taken-title-2");
	}

	@Test
	void updateChecksExistenceThenOwnershipThenValidation() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Mine");

		assertThat(put(ARTICLES + "/" + slug, null, "{\"article\":{\"body\":\"x\"}}").status()).isEqualTo(401);

		Resp missing = put(ARTICLES + "/nope", bob, "{\"article\":{\"title\":\"\"}}");
		assertThat(missing.status()).isEqualTo(404);

		Resp forbidden = put(ARTICLES + "/" + slug, bob, "{\"article\":{\"title\":\"\"}}");
		assertThat(forbidden.status()).isEqualTo(403);
		assertThat((String) forbidden.json("$.errors.article[0]")).isEqualTo("forbidden");

		Resp invalid = put(ARTICLES + "/" + slug, alice, "{\"article\":{\"title\":\"\",\"body\":null}}");
		assertThat(invalid.status()).isEqualTo(422);
		assertThat((Object) invalid.json("$.errors.title")).isNotNull();
		assertThat((Object) invalid.json("$.errors.body")).isNotNull();
	}


	@Test
	void deleteReturns204WithoutABodyAndWithoutRequiringAContentType() {
		String token = register("alice");
		String slug = createArticle(token, "Doomed");

		Resp r = send("DELETE", ARTICLES + "/" + slug, token, null, null);
		assertThat(r.status()).isEqualTo(204);
		assertThat(r.body()).isEmpty();
		assertThat(get(ARTICLES + "/" + slug, null).status()).isEqualTo(404);
	}

	@Test
	void deleteChecksAuthenticationExistenceAndOwnership() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Protected");

		assertThat(delete(ARTICLES + "/" + slug, null).status()).isEqualTo(401);
		assertThat(delete(ARTICLES + "/nope", bob).status()).isEqualTo(404);
		Resp forbidden = delete(ARTICLES + "/" + slug, bob);
		assertThat(forbidden.status()).isEqualTo(403);
		assertThat((String) forbidden.json("$.errors.article[0]")).isEqualTo("forbidden");
		assertThat(get(ARTICLES + "/" + slug, null).status()).isEqualTo(200);
	}

	@Test
	void deletingAnArticleRemovesItsCommentsFavoritesAndTags() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Gone", "t1");
		post(ARTICLES + "/" + slug + "/favorite", bob, null);
		post(ARTICLES + "/" + slug + "/comments", bob, "{\"comment\":{\"body\":\"hi\"}}");

		assertThat(delete(ARTICLES + "/" + slug, alice).status()).isEqualTo(204);

		assertThat(jdbc.sql("select count(*) from favorites").query(Long.class).single()).isZero();
		assertThat(jdbc.sql("select count(*) from comments").query(Long.class).single()).isZero();
		assertThat(jdbc.sql("select count(*) from article_tags").query(Long.class).single()).isZero();
	}


	@Test
	void favoriteAndUnfavoriteAreIdempotentAndReturnTheUpdatedArticle() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Popular");

		for (int i = 0; i < 2; i++) {
			Resp fav = post(ARTICLES + "/" + slug + "/favorite", bob, null);
			assertThat(fav.status()).isEqualTo(200);
			assertThat((Boolean) fav.json("$.article.favorited")).isTrue();
			assertThat((Integer) fav.json("$.article.favoritesCount")).isEqualTo(1);
		}
		Resp second = post(ARTICLES + "/" + slug + "/favorite", alice, null);
		assertThat((Integer) second.json("$.article.favoritesCount")).isEqualTo(2);

		for (int i = 0; i < 2; i++) {
			Resp unfav = delete(ARTICLES + "/" + slug + "/favorite", bob);
			assertThat(unfav.status()).isEqualTo(200);
			assertThat((Boolean) unfav.json("$.article.favorited")).isFalse();
			assertThat((Integer) unfav.json("$.article.favoritesCount")).isEqualTo(1);
		}
	}

	@Test
	void favoriteRequiresAuthenticationAndAnExistingArticle() {
		String alice = register("alice");
		String slug = createArticle(alice, "Exists");

		assertThat(post(ARTICLES + "/" + slug + "/favorite", null, null).status()).isEqualTo(401);
		assertThat(delete(ARTICLES + "/" + slug + "/favorite", null).status()).isEqualTo(401);
		Resp missing = post(ARTICLES + "/nope/favorite", alice, null);
		assertThat(missing.status()).isEqualTo(404);
		assertThat((String) missing.json("$.errors.article[0]")).isEqualTo("not found");
	}


	@Test
	void tagsAreRankedByUsageThenAlphabeticallyAndNeedNoAuth() {
		String token = register("alice");
		createArticle(token, "One", "beta", "alpha", "gamma");
		createArticle(token, "Two", "beta", "alpha");
		createArticle(token, "Three", "beta", "delta");

		Resp r = get(V1 + "/tags", null);
		assertThat(r.status()).isEqualTo(200);
		assertThat((List<String>) r.json("$.tags")).containsExactly("beta", "alpha", "delta", "gamma");
	}

	@Test
	void tagsAreLimitedToTheTwentyMostUsed() {
		String token = register("alice");
		for (int i = 0; i < 25; i++) {
			createArticle(token, "Article " + i, String.format("tag%02d", i));
		}
		List<String> tags = get(V1 + "/tags", null).json("$.tags");
		assertThat(tags).hasSize(20);
		assertThat(tags.get(0)).isEqualTo("tag00");
	}

	@Test
	void tagsOfAnEmptySystemIsAnEmptyList() {
		Resp r = get(V1 + "/tags", null);
		assertThat(r.status()).isEqualTo(200);
		assertThat((List<?>) r.json("$.tags")).isEmpty();
	}
}
