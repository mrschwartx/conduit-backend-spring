package com.conduit.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.conduit.backend.support.AbstractApiTest;
import com.conduit.backend.shared.security.JwtService;

class IdentityApiTest extends AbstractApiTest {

	private static final String REGISTER = V1 + "/users";

	private static String user(String username, String email, String password) {
		return """
				{"user":{"username":"%s","email":"%s","password":"%s"}}""".formatted(username, email, password);
	}

	@Test
	void registerReturns201WithTokenAndNullBioAndImage() {
		Resp r = post(REGISTER, null, user("alice", "Alice@Example.com", PASSWORD));

		assertThat(r.status()).isEqualTo(201);
		assertThat((String) r.json("$.user.username")).isEqualTo("alice");
		assertThat((String) r.json("$.user.email")).isEqualTo("alice@example.com");
		assertThat((String) r.json("$.user.token")).isNotBlank();
		Map<String, Object> userObject = r.json("$.user");
		assertThat(userObject).containsKeys("bio", "image").containsEntry("bio", null).containsEntry("image", null);
		assertThat(userObject).doesNotContainKeys("password", "passwordHash", "id");
		assertThat(r.header("Content-Type")).startsWith("application/json");
	}

	@Test
	void registerReportsAllInvalidFieldsAtOnce() {
		Resp r = post(REGISTER, null, user("a!", "not-an-email", "short"));

		assertThat(r.status()).isEqualTo(422);
		assertThat((Object) r.json("$.errors.username")).isNotNull();
		assertThat((Object) r.json("$.errors.email")).isNotNull();
		assertThat((Object) r.json("$.errors.password")).isNotNull();
	}

	@Test
	void registerWithoutEnvelopeIs422() {
		Resp r = post(REGISTER, null, "{}");
		assertThat(r.status()).isEqualTo(422);
		assertThat((String) r.json("$.errors.user[0]")).isEqualTo("can't be blank");
	}

	@Test
	void duplicateUsernameAndEmailAreConflictsIgnoringCase() {
		register("alice");

		Resp byUsername = post(REGISTER, null, user("ALICE", "other@example.com", PASSWORD));
		assertThat(byUsername.status()).isEqualTo(409);
		assertThat((String) byUsername.json("$.errors.username[0]")).isEqualTo("has already been taken");

		Resp byEmail = post(REGISTER, null, user("bob", "ALICE@example.com", PASSWORD));
		assertThat(byEmail.status()).isEqualTo(409);
		assertThat((String) byEmail.json("$.errors.email[0]")).isEqualTo("has already been taken");
	}

	@Test
	void malformedJsonIs400() {
		Resp r = send("POST", REGISTER, null, "{not json", "application/json");
		assertThat(r.status()).isEqualTo(400);
		assertThat((String) r.json("$.errors.body[0]")).isEqualTo("is not valid JSON");
	}

	@Test
	void bodyWithWrongContentTypeIs415() {
		Resp r = send("POST", REGISTER, null, user("alice", "a@example.com", PASSWORD), "text/plain");
		assertThat(r.status()).isEqualTo(415);
		assertThat((Object) r.json("$.errors.body")).isNotNull();
	}

	@Test
	void oversizedBodyIs413() {
		String huge = "{\"user\":{\"username\":\"" + "a".repeat(1_100_000) + "\"}}";
		Resp r = post(REGISTER, null, huge);
		assertThat(r.status()).isEqualTo(413);
	}

	@Test
	void loginSucceedsCaseInsensitivelyByEmail() {
		register("alice");
		Resp r = post(V1 + "/users/login", null, """
				{"user":{"email":"ALICE@example.com","password":"%s"}}""".formatted(PASSWORD));
		assertThat(r.status()).isEqualTo(200);
		assertThat((String) r.json("$.user.username")).isEqualTo("alice");
		assertThat((String) r.json("$.user.token")).isNotBlank();
	}

	@Test
	void loginFailureIsIdenticalForUnknownEmailAndWrongPassword() {
		register("alice");
		Resp wrongPassword = post(V1 + "/users/login", null, """
				{"user":{"email":"alice@example.com","password":"wrong-password"}}""");
		Resp unknownEmail = post(V1 + "/users/login", null, """
				{"user":{"email":"nobody@example.com","password":"wrong-password"}}""");

		assertThat(wrongPassword.status()).isEqualTo(401);
		assertThat(unknownEmail.status()).isEqualTo(401);
		assertThat(wrongPassword.body()).isEqualTo(unknownEmail.body());
		assertThat((String) wrongPassword.json("$.errors.credentials[0]")).isEqualTo("are invalid");
	}

	@Test
	void loginRequiresFields() {
		Resp r = post(V1 + "/users/login", null, "{\"user\":{}}");
		assertThat(r.status()).isEqualTo(422);
		assertThat((Object) r.json("$.errors.email")).isNotNull();
		assertThat((Object) r.json("$.errors.password")).isNotNull();
	}

	@Test
	void currentUserRequiresAValidTokenWithTheTokenScheme() {
		String token = register("alice");

		Resp ok = get(V1 + "/user", token);
		assertThat(ok.status()).isEqualTo(200);
		assertThat((String) ok.json("$.user.email")).isEqualTo("alice@example.com");

		Resp missing = get(V1 + "/user", null);
		assertThat(missing.status()).isEqualTo(401);
		assertThat((String) missing.json("$.errors.token[0]")).isEqualTo("is missing");

		Resp garbage = get(V1 + "/user", "garbage");
		assertThat(garbage.status()).isEqualTo(401);
		assertThat((String) garbage.json("$.errors.token[0]")).isEqualTo("is invalid");

		Resp bearer = sendRaw("GET", V1 + "/user", "Bearer " + token, null, null);
		assertThat(bearer.status()).isEqualTo(401);
		assertThat((String) bearer.json("$.errors.token[0]")).isEqualTo("is invalid");
	}

	@Test
	void expiredTokenIs401HasExpired() {
		register("alice");
		JwtService past = new JwtService(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8), Duration.ofMinutes(1),
				Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC));
		long id = jdbc.sql("select id from users where username = 'alice'").query(Long.class).single();

		Resp r = get(V1 + "/user", past.issue(id, "alice"));
		assertThat(r.status()).isEqualTo(401);
		assertThat((String) r.json("$.errors.token[0]")).isEqualTo("has expired");
	}

	@Test
	void tokenForADeletedUserIs401() {
		String token = register("alice");
		jdbc.sql("delete from users").update();
		assertThat(get(V1 + "/user", token).status()).isEqualTo(401);
	}

	@Test
	void partialUpdateChangesOnlyTheSentFields() {
		String token = register("alice");

		Resp r = put(V1 + "/user", token, "{\"user\":{\"bio\":\"hello\",\"image\":\"https://img.example.com/a.png\"}}");
		assertThat(r.status()).isEqualTo(200);
		assertThat((String) r.json("$.user.bio")).isEqualTo("hello");
		assertThat((String) r.json("$.user.username")).isEqualTo("alice");
		assertThat((String) r.json("$.user.email")).isEqualTo("alice@example.com");

		// null clears, absent leaves untouched
		Resp cleared = put(V1 + "/user", token, "{\"user\":{\"bio\":null}}");
		assertThat(cleared.status()).isEqualTo(200);
		Map<String, Object> user = cleared.json("$.user");
		assertThat(user).containsEntry("bio", null);
		assertThat(user).containsEntry("image", "https://img.example.com/a.png");

		// "" also clears
		Resp emptied = put(V1 + "/user", token, "{\"user\":{\"image\":\"\"}}");
		Map<String, Object> user2 = emptied.json("$.user");
		assertThat(user2).containsEntry("image", null);
	}

	@Test
	void updateCanChangeUsernameEmailAndPassword() {
		String token = register("alice");

		Resp r = put(V1 + "/user", token, """
				{"user":{"username":"alice2","email":"New@Example.com","password":"another-password"}}""");
		assertThat(r.status()).isEqualTo(200);
		assertThat((String) r.json("$.user.username")).isEqualTo("alice2");
		assertThat((String) r.json("$.user.email")).isEqualTo("new@example.com");

		Resp login = post(V1 + "/users/login", null, """
				{"user":{"email":"new@example.com","password":"another-password"}}""");
		assertThat(login.status()).isEqualTo(200);
	}

	@Test
	void updateRequiresAtLeastOneFieldAndValidatesThem() {
		String token = register("alice");

		assertThat(put(V1 + "/user", token, "{\"user\":{}}").status()).isEqualTo(422);
		assertThat(put(V1 + "/user", token, "{\"user\":{\"unknown\":1}}").status()).isEqualTo(422);

		Resp invalid = put(V1 + "/user", token, "{\"user\":{\"image\":\"http://insecure.example.com/a.png\",\"bio\":\""
				+ "x".repeat(301) + "\"}}");
		assertThat(invalid.status()).isEqualTo(422);
		assertThat((Object) invalid.json("$.errors.image")).isNotNull();
		assertThat((Object) invalid.json("$.errors.bio")).isNotNull();
	}

	@Test
	void updateIgnoresOwnRecordForUniquenessButConflictsWithOthers() {
		String token = register("alice");
		register("bob");

		Resp same = put(V1 + "/user", token, "{\"user\":{\"username\":\"alice\",\"email\":\"alice@example.com\"}}");
		assertThat(same.status()).isEqualTo(200);

		Resp taken = put(V1 + "/user", token, "{\"user\":{\"username\":\"BOB\",\"email\":\"bob@example.com\"}}");
		assertThat(taken.status()).isEqualTo(409);
		assertThat((Object) taken.json("$.errors.username")).isNotNull();
		assertThat((Object) taken.json("$.errors.email")).isNotNull();
	}

	@Test
	void updateRequiresAuthentication() {
		Resp r = put(V1 + "/user", null, "{\"user\":{\"bio\":\"x\"}}");
		assertThat(r.status()).isEqualTo(401);
	}
}
