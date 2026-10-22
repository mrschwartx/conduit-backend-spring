package com.conduit.backend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.conduit.backend.shared.security.InvalidTokenException;
import com.conduit.backend.shared.security.JwtService;

class JwtServiceTest {

	private static final byte[] SECRET = "unit-test-secret-0123456789-abcdefgh".getBytes(StandardCharsets.UTF_8);
	private static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");

	private static JwtService serviceAt(Instant now, byte[] secret) {
		return new JwtService(secret, Duration.ofMinutes(10), Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void issuedTokenVerifiesAndReturnsUserId() throws Exception {
		JwtService jwt = serviceAt(T0, SECRET);
		assertThat(jwt.verify(jwt.issue(42, "alice"))).isEqualTo(42L);
	}

	@Test
	void expiredTokenIsReportedAsExpired() {
		String token = serviceAt(T0, SECRET).issue(1, "alice");
		JwtService later = serviceAt(T0.plus(Duration.ofMinutes(11)), SECRET);
		assertThatThrownBy(() -> later.verify(token))
				.isInstanceOfSatisfying(InvalidTokenException.class, ex -> assertThat(ex.isExpired()).isTrue());
	}

	@Test
	void tokenSignedWithAnotherSecretIsInvalid() {
		String token = serviceAt(T0, "another-secret-0123456789-abcdefghijk".getBytes(StandardCharsets.UTF_8))
				.issue(1, "alice");
		assertThatThrownBy(() -> serviceAt(T0, SECRET).verify(token))
				.isInstanceOfSatisfying(InvalidTokenException.class, ex -> assertThat(ex.isExpired()).isFalse());
	}

	@Test
	void tamperedPayloadIsInvalid() {
		JwtService jwt = serviceAt(T0, SECRET);
		String[] parts = jwt.issue(1, "alice").split("\\.");
		String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
				"{\"sub\":\"2\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));
		String forged = parts[0] + "." + forgedPayload + "." + parts[2];
		assertThatThrownBy(() -> jwt.verify(forged)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void unsignedAlgNoneTokenIsRejected() {
		Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
		String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = enc.encodeToString("{\"sub\":\"1\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> serviceAt(T0, SECRET).verify(header + "." + payload + "."))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void garbageIsInvalid() {
		JwtService jwt = serviceAt(T0, SECRET);
		assertThatThrownBy(() -> jwt.verify("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> jwt.verify("a.b.c")).isInstanceOf(InvalidTokenException.class);
	}
}
