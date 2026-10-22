package com.conduit.backend.shared.security;

public record AuthUser(long id, String username) {

	public static final long ANONYMOUS_ID = 0L;

	public static long idOrAnonymous(AuthUser user) {
		return user == null ? ANONYMOUS_ID : user.id();
	}
}
