package com.conduit.backend.shared.web;

import java.io.IOException;

public class BodyTooLargeException extends IOException {

	public BodyTooLargeException(long limit) {
		super("request body exceeds " + limit + " bytes");
	}
}
