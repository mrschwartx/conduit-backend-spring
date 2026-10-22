package com.conduit.backend.shared.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Generates or propagates X-Request-Id, puts it in the MDC (so it appears in every
 * JSON log line), echoes it in the response and writes one access-log line per request.
 * Runs before the Spring Security filter chain so that 401/403 responses are logged too.
 * Only the path is logged (no query string) and never the Authorization header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	public static final String MDC_KEY = "requestId";

	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private static final Logger accessLog = LoggerFactory.getLogger("access");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String requestId = incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming
				: UUID.randomUUID().toString();
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			long millis = (System.nanoTime() - start) / 1_000_000;
			accessLog.atInfo()
					.addKeyValue("method", request.getMethod())
					.addKeyValue("path", request.getRequestURI())
					.addKeyValue("status", response.getStatus())
					.addKeyValue("durationMs", millis)
					.log("request completed");
			MDC.remove(MDC_KEY);
		}
	}
}
