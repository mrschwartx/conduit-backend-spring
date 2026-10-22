package com.conduit.backend.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.conduit.backend.shared.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final String ALGORITHM = "HS256";

	private final SecretKey key;
	private final Duration ttl;
	private final Clock clock;
	private final JwtParser parser;

	@Autowired
	public JwtService(AppProperties properties, Clock clock) {
		this(properties.jwtSecret().getBytes(StandardCharsets.UTF_8),
				Duration.ofMinutes(properties.jwtTtlMinutes()), clock);
	}

	public JwtService(byte[] secret, Duration ttl, Clock clock) {
		this.key = Keys.hmacShaKeyFor(secret);
		this.ttl = ttl;
		this.clock = clock;
		this.parser = Jwts.parser()
				.verifyWith(key)
				.clock(() -> Date.from(clock.instant()))
				.build();
	}

	public String issue(long userId, String username) {
		Instant now = clock.instant();
		return Jwts.builder()
				.subject(Long.toString(userId))
				.claim("username", username)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttl)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public long verify(String token) throws InvalidTokenException {
		try {
			Jws<Claims> jws = parser.parseSignedClaims(token);
			if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
				throw new InvalidTokenException("unsupported algorithm", false);
			}
			Claims claims = jws.getPayload();
			if (claims.getExpiration() == null) {
				throw new InvalidTokenException("no expiry", false);
			}
			String subject = claims.getSubject();
			if (subject == null) {
				throw new InvalidTokenException("no subject", false);
			}
			return Long.parseLong(subject);
		}
		catch (ExpiredJwtException ex) {
			throw new InvalidTokenException("expired", true);
		}
		catch (JwtException | IllegalArgumentException ex) {
			throw new InvalidTokenException("malformed token", false);
		}
	}
}
