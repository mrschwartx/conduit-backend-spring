package com.conduit.backend.shared.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.conduit.backend.shared.errors.ErrorJson;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String SCHEME = "Token ";

	private final JwtService jwtService;
	private final AuthUserLookup users;

	public TokenAuthenticationFilter(JwtService jwtService, AuthUserLookup users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null) {
			chain.doFilter(request, response);
			return;
		}
		if (!header.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
			reject(response, "is invalid");
			return;
		}
		String token = header.substring(SCHEME.length()).strip();
		if (token.isEmpty()) {
			reject(response, "is invalid");
			return;
		}
		long userId;
		try {
			userId = jwtService.verify(token);
		}
		catch (InvalidTokenException ex) {
			reject(response, ex.isExpired() ? "has expired" : "is invalid");
			return;
		}
		Optional<String> username = users.findUsernameById(userId);
		if (username.isEmpty()) {
			reject(response, "is invalid");
			return;
		}
		AuthUser principal = new AuthUser(userId, username.get());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
		SecurityContextHolder.setContext(context);
		chain.doFilter(request, response);
	}

	private static void reject(HttpServletResponse response, String message) throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Token");
		ErrorJson.write(response, 401, Map.of("token", List.of(message)));
	}
}
