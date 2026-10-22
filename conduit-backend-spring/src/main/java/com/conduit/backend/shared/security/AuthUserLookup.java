package com.conduit.backend.shared.security;

import java.util.Optional;

/** Lets the token filter confirm that the user named by a token still exists. */
public interface AuthUserLookup {

	/** The current username for the id, or empty if no such user exists. */
	Optional<String> findUsernameById(long id);
}
