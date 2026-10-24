package com.conduit.backend.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "conduit.security.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
