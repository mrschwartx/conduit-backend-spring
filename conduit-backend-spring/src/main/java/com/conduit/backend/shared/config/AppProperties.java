package com.conduit.backend.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings, bound from {@code conduit.*} (environment variables are
 * mapped in application.yaml). Invalid settings fail the application at startup.
 */
@ConfigurationProperties(prefix = "conduit")
public record AppProperties(
		String jwtSecret,
		int jwtTtlMinutes,
		List<String> corsAllowedOrigins,
		long maxBodyBytes) {

	public AppProperties {
		if (jwtSecret == null || jwtSecret.length() < 32) {
			throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
		}
		if (jwtTtlMinutes < 1) {
			throw new IllegalStateException("JWT_TTL_MINUTES must be >= 1");
		}
		if (maxBodyBytes < 1) {
			maxBodyBytes = 1_048_576L;
		}
		corsAllowedOrigins = corsAllowedOrigins == null ? List.of()
				: corsAllowedOrigins.stream()
						.map(String::strip)
						.filter(o -> !o.isEmpty())
						.map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
						.toList();
	}
}
