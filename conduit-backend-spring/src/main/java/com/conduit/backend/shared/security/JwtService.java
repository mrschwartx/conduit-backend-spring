package com.conduit.backend.shared.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.conduit.backend.shared.config.AppProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Issues and verifies HS256 JWTs. Claims: sub (user id), username, iat, exp.
 * Implemented directly on Nimbus JOSE so the "Authorization: Token" scheme, the
 * contract-shaped 401 messages (missing / invalid / expired) and the strict
 * HS256-only policy stay fully under control.
 */
@Service
public class JwtService {

	private final byte[] secret;
	private final Duration ttl;
	private final Clock clock;

	@Autowired
	public JwtService(AppProperties properties, Clock clock) {
		this(properties.jwtSecret().getBytes(StandardCharsets.UTF_8),
				Duration.ofMinutes(properties.jwtTtlMinutes()), clock);
	}

	public JwtService(byte[] secret, Duration ttl, Clock clock) {
		this.secret = secret.clone();
		this.ttl = ttl;
		this.clock = clock;
	}

	public String issue(long userId, String username) {
		Instant now = clock.instant();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(Long.toString(userId))
				.claim("username", username)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plus(ttl)))
				.build();
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret));
			return jwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Unable to sign JWT", ex);
		}
	}

	/** Verifies signature, algorithm and expiry; returns the user id from {@code sub}. */
	public long verify(String token) throws InvalidTokenException {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
				throw new InvalidTokenException("unsupported algorithm", false);
			}
			if (!jwt.verify(new MACVerifier(secret))) {
				throw new InvalidTokenException("bad signature", false);
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Date expiry = claims.getExpirationTime();
			if (expiry == null) {
				throw new InvalidTokenException("no expiry", false);
			}
			if (!expiry.toInstant().isAfter(clock.instant())) {
				throw new InvalidTokenException("expired", true);
			}
			String subject = claims.getSubject();
			if (subject == null) {
				throw new InvalidTokenException("no subject", false);
			}
			return Long.parseLong(subject);
		}
		catch (ParseException | JOSEException | NumberFormatException ex) {
			throw new InvalidTokenException("malformed token", false);
		}
	}
}
