package com.conduit.backend.shared.security;

public class InvalidTokenException extends Exception {

	private final boolean expired;

	public InvalidTokenException(String message, boolean expired) {
		super(message, null, false, false);
		this.expired = expired;
	}

	public boolean isExpired() {
		return expired;
	}
}
