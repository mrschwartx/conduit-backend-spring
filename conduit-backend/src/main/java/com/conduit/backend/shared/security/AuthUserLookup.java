package com.conduit.backend.shared.security;

import java.util.Optional;

public interface AuthUserLookup {

	Optional<String> findUsernameById(long id);
}
