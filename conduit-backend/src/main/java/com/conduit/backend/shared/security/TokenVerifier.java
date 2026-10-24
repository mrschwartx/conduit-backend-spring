package com.conduit.backend.shared.security;

import java.util.Optional;

public interface TokenVerifier {

    Optional<AuthenticatedUser> verify(String token);
}
