package com.conduit.backend.shared.security;

/** The authenticated caller, as established from a verified JWT. */
public record AuthUser(long id, String username) {

	/** Id used for anonymous callers; no user ever has it (identity columns start at 1). */
	public static final long ANONYMOUS_ID = 0L;

	public static long idOrAnonymous(AuthUser user) {
		return user == null ? ANONYMOUS_ID : user.id();
	}
}
