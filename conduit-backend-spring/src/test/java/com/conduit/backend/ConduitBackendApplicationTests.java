package com.conduit.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.conduit.backend.support.AbstractApiTest;

class ConduitBackendApplicationTests extends AbstractApiTest {

	@Test
	void contextLoadsAndHealthIsOutsideTheVersionedPrefix() {
		var health = get("/health", null);
		assertThat(health.status()).isEqualTo(200);
		assertThat(health.body()).isEqualTo("OK");
		assertThat(health.header("X-Request-Id")).isNotBlank();
	}

	@Test
	void unknownPathIsAContractShaped404() {
		var response = get("/nope", null);
		assertThat(response.status()).isEqualTo(404);
		assertThat((Object) response.json("$.errors.path[0]")).isEqualTo("not found");
	}
}
