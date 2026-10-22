package com.conduit.backend.shared.web;

import java.io.IOException;

/** Thrown while reading a request body that exceeds the configured limit. */
public class BodyTooLargeException extends IOException {

	public BodyTooLargeException(long limit) {
		super("request body exceeds " + limit + " bytes");
	}
}
