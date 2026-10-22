package com.conduit.backend.profiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.conduit.backend.support.AbstractApiTest;

class ProfilesApiTest extends AbstractApiTest {

	@Test
	void anonymousCanReadAProfileAndFollowingIsFalse() {
		register("alice");

		Resp r = get(V1 + "/profiles/alice", null);
		assertThat(r.status()).isEqualTo(200);
		assertThat((String) r.json("$.profile.username")).isEqualTo("alice");
		assertThat((Boolean) r.json("$.profile.following")).isFalse();
		Map<String, Object> profile = r.json("$.profile");
		assertThat(profile).containsEntry("bio", null).containsEntry("image", null);
		assertThat(profile).doesNotContainKeys("email", "token");
	}

	@Test
	void usernameLookupIsCaseInsensitive() {
		register("alice");
		assertThat(get(V1 + "/profiles/ALICE", null).status()).isEqualTo(200);
	}

	@Test
	void unknownProfileIs404() {
		Resp r = get(V1 + "/profiles/ghost", null);
		assertThat(r.status()).isEqualTo(404);
		assertThat((String) r.json("$.errors.profile[0]")).isEqualTo("not found");
	}

	@Test
	void invalidTokenOnAnOptionalEndpointIs401() {
		register("alice");
		Resp r = get(V1 + "/profiles/alice", "garbage");
		assertThat(r.status()).isEqualTo(401);
		assertThat((String) r.json("$.errors.token[0]")).isEqualTo("is invalid");
	}

	@Test
	void followAndUnfollowAreIdempotentAndReturnTheUpdatedProfile() {
		String alice = register("alice");
		register("bob");

		for (int i = 0; i < 2; i++) {
			Resp follow = post(V1 + "/profiles/bob/follow", alice, null);
			assertThat(follow.status()).isEqualTo(200);
			assertThat((Boolean) follow.json("$.profile.following")).isTrue();
		}
		assertThat((Boolean) get(V1 + "/profiles/bob", alice).json("$.profile.following")).isTrue();
		assertThat((Boolean) get(V1 + "/profiles/bob", null).json("$.profile.following")).isFalse();

		for (int i = 0; i < 2; i++) {
			Resp unfollow = delete(V1 + "/profiles/bob/follow", alice);
			assertThat(unfollow.status()).isEqualTo(200);
			assertThat((Boolean) unfollow.json("$.profile.following")).isFalse();
		}
	}

	@Test
	void followingYourselfIs422() {
		String alice = register("alice");
		Resp r = post(V1 + "/profiles/alice/follow", alice, null);
		assertThat(r.status()).isEqualTo(422);
		assertThat((String) r.json("$.errors.username[0]")).isEqualTo("you cannot follow yourself");
	}

	@Test
	void followRequiresAuthenticationAndAnExistingUser() {
		register("alice");
		assertThat(post(V1 + "/profiles/alice/follow", null, null).status()).isEqualTo(401);
		assertThat(delete(V1 + "/profiles/alice/follow", null).status()).isEqualTo(401);

		String bob = register("bob");
		Resp missing = post(V1 + "/profiles/ghost/follow", bob, null);
		assertThat(missing.status()).isEqualTo(404);
		assertThat((String) missing.json("$.errors.profile[0]")).isEqualTo("not found");
	}
}
