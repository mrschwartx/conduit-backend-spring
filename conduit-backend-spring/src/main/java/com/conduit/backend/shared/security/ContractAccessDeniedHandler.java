package com.conduit.backend.shared.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.conduit.backend.shared.errors.ErrorJson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Contract-shaped 403 for anything Spring Security itself denies (resource ownership is checked in services). */
@Component
public class ContractAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ErrorJson.write(response, 403, Map.of("request", List.of("forbidden")));
	}
}
