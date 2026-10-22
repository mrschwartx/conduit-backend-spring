package com.conduit.backend.comments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.conduit.backend.support.AbstractApiTest;

class CommentsApiTest extends AbstractApiTest {

	private static String commentJson(String body) {
		return "{\"comment\":{\"body\":\"" + body + "\"}}";
	}

	private long addComment(String token, String slug, String body) {
		Resp r = post(V1 + "/articles/" + slug + "/comments", token, commentJson(body));
		assertThat(r.status()).isEqualTo(201);
		return ((Number) r.json("$.comment.id")).longValue();
	}

	@Test
	void anAuthenticatedUserCanCommentAndGetsTheContractShape() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");

		Resp r = post(V1 + "/articles/" + slug + "/comments", alice, commentJson("Nice post"));

		assertThat(r.status()).isEqualTo(201);
		assertThat((Object) r.json("$.comment.id")).isInstanceOf(Integer.class);
		assertThat((String) r.json("$.comment.body")).isEqualTo("Nice post");
		assertThat((String) r.json("$.comment.createdAt")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
		assertThat((String) r.json("$.comment.updatedAt")).isNotBlank();
		assertThat((String) r.json("$.comment.author.username")).isEqualTo("alice");
		assertThat((Boolean) r.json("$.comment.author.following")).isFalse();
		Map<String, Object> author = r.json("$.comment.author");
		assertThat(author).containsEntry("bio", null).containsEntry("image", null);
	}

	@Test
	void commentsAreListedNewestFirstAndAnonymousCanRead() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Hello");
		long first = addComment(alice, slug, "first");
		long second = addComment(bob, slug, "second");
		long third = addComment(alice, slug, "third");

		Resp r = get(V1 + "/articles/" + slug + "/comments", null);

		assertThat(r.status()).isEqualTo(200);
		List<Integer> ids = r.json("$.comments[*].id");
		assertThat(ids).containsExactly((int) third, (int) second, (int) first);
		List<String> bodies = r.json("$.comments[*].body");
		assertThat(bodies).containsExactly("third", "second", "first");
	}

	@Test
	void anArticleWithoutCommentsHasAnEmptyList() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");

		Resp r = get(V1 + "/articles/" + slug + "/comments", null);

		assertThat(r.status()).isEqualTo(200);
		List<Object> comments = r.json("$.comments");
		assertThat(comments).isEmpty();
	}

	@Test
	void followingIsComputedRelativeToTheViewer() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Hello");
		addComment(bob, slug, "from bob");
		assertThat(post(V1 + "/profiles/bob/follow", alice, null).status()).isEqualTo(200);

		assertThat((Boolean) get(V1 + "/articles/" + slug + "/comments", alice).json("$.comments[0].author.following"))
				.isTrue();
		assertThat((Boolean) get(V1 + "/articles/" + slug + "/comments", null).json("$.comments[0].author.following"))
				.isFalse();
	}

	@Test
	void invalidTokenOnTheOptionalListEndpointIs401() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");

		Resp r = get(V1 + "/articles/" + slug + "/comments", "garbage");

		assertThat(r.status()).isEqualTo(401);
		assertThat((String) r.json("$.errors.token[0]")).isEqualTo("is invalid");
	}

	@Test
	void unknownArticleIs404ForEveryCommentOperation() {
		String alice = register("alice");

		Resp list = get(V1 + "/articles/nope/comments", null);
		assertThat(list.status()).isEqualTo(404);
		assertThat((String) list.json("$.errors.article[0]")).isEqualTo("not found");
		assertThat(post(V1 + "/articles/nope/comments", alice, commentJson("x")).status()).isEqualTo(404);
		assertThat(delete(V1 + "/articles/nope/comments/1", alice).status()).isEqualTo(404);
	}

	@Test
	void writingRequiresAuthentication() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");
		long id = addComment(alice, slug, "mine");

		Resp add = post(V1 + "/articles/" + slug + "/comments", null, commentJson("anon"));
		assertThat(add.status()).isEqualTo(401);
		assertThat((String) add.json("$.errors.token[0]")).isEqualTo("is missing");
		assertThat(delete(V1 + "/articles/" + slug + "/comments/" + id, null).status()).isEqualTo(401);
	}

	@Test
	void bodyIsValidated() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");
		String path = V1 + "/articles/" + slug + "/comments";

		Resp blank = post(path, alice, commentJson("   "));
		assertThat(blank.status()).isEqualTo(422);
		assertThat((String) blank.json("$.errors.body[0]")).isEqualTo("can't be blank");

		Resp missing = post(path, alice, "{\"comment\":{}}");
		assertThat(missing.status()).isEqualTo(422);
		assertThat((String) missing.json("$.errors.body[0]")).isEqualTo("can't be blank");

		Resp noEnvelope = post(path, alice, "{\"body\":\"flat\"}");
		assertThat(noEnvelope.status()).isEqualTo(422);

		Resp tooLong = post(path, alice, commentJson("x".repeat(1001)));
		assertThat(tooLong.status()).isEqualTo(422);
		assertThat((String) tooLong.json("$.errors.body[0]")).isEqualTo("is too long (maximum is 1000 characters)");

		assertThat(post(path, alice, commentJson("x".repeat(1000))).status()).isEqualTo(201);
		// the length limit counts characters, not bytes
		assertThat(post(path, alice, commentJson("é".repeat(1000))).status()).isEqualTo(201);
	}

	@Test
	void contentTypeAndJsonErrors() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");
		String path = V1 + "/articles/" + slug + "/comments";

		Resp wrongType = send("POST", path, alice, commentJson("x"), "text/plain");
		assertThat(wrongType.status()).isEqualTo(415);

		Resp malformed = post(path, alice, "{not json");
		assertThat(malformed.status()).isEqualTo(400);
		assertThat((String) malformed.json("$.errors.body[0]")).isEqualTo("is not valid JSON");
	}

	@Test
	void onlyTheAuthorCanDeleteAComment() {
		String alice = register("alice");
		String bob = register("bob");
		String slug = createArticle(alice, "Hello");
		long id = addComment(bob, slug, "from bob");
		String path = V1 + "/articles/" + slug + "/comments/" + id;

		// even the article's author may not delete somebody else's comment
		Resp forbidden = delete(path, alice);
		assertThat(forbidden.status()).isEqualTo(403);
		assertThat((String) forbidden.json("$.errors.comment[0]")).isEqualTo("forbidden");
		assertThat(get(V1 + "/articles/" + slug + "/comments", null).<List<Object>>json("$.comments")).hasSize(1);

		Resp deleted = delete(path, bob);
		assertThat(deleted.status()).isEqualTo(204);
		assertThat(deleted.body()).isEmpty();
		assertThat(get(V1 + "/articles/" + slug + "/comments", null).<List<Object>>json("$.comments")).isEmpty();

		// the comment is gone now
		assertThat(delete(path, bob).status()).isEqualTo(404);
	}

	@Test
	void aCommentIdThatBelongsToAnotherArticleIs404() {
		String alice = register("alice");
		String slugA = createArticle(alice, "Article A");
		String slugB = createArticle(alice, "Article B");
		long id = addComment(alice, slugA, "on A");

		Resp r = delete(V1 + "/articles/" + slugB + "/comments/" + id, alice);

		assertThat(r.status()).isEqualTo(404);
		assertThat((String) r.json("$.errors.comment[0]")).isEqualTo("not found");
		// ... and it was not deleted
		assertThat(get(V1 + "/articles/" + slugA + "/comments", null).<List<Object>>json("$.comments")).hasSize(1);
	}

	@Test
	void unknownAndMalformedCommentIds() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");

		assertThat(delete(V1 + "/articles/" + slug + "/comments/9999", alice).status()).isEqualTo(404);

		Resp malformed = delete(V1 + "/articles/" + slug + "/comments/abc", alice);
		assertThat(malformed.status()).isEqualTo(422);
		assertThat((String) malformed.json("$.errors.id[0]")).isEqualTo("is invalid");
	}

	@Test
	void deletingAnArticleDeletesItsComments() {
		String alice = register("alice");
		String slug = createArticle(alice, "Hello");
		addComment(alice, slug, "one");
		addComment(alice, slug, "two");

		assertThat(delete(V1 + "/articles/" + slug, alice).status()).isEqualTo(204);

		assertThat(jdbc.sql("select count(*) from comments").query(Long.class).single()).isZero();
	}

	@Test
	void listResolvesTheAuthorOfEveryComment() {
		// 25 comments by 5 different authors: every one comes back with its author resolved
		String owner = register("owner");
		String slug = createArticle(owner, "Busy");
		for (int u = 0; u < 5; u++) {
			String token = register("user" + u);
			for (int c = 0; c < 5; c++) {
				addComment(token, slug, "c" + u + "-" + c);
			}
		}

		Resp r = get(V1 + "/articles/" + slug + "/comments", owner);

		List<String> authors = r.json("$.comments[*].author.username");
		assertThat(authors).hasSize(25).doesNotContainNull();
		assertThat(authors).containsOnly("user0", "user1", "user2", "user3", "user4");
	}
}
