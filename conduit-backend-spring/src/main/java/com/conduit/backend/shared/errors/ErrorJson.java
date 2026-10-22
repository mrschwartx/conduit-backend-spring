package com.conduit.backend.shared.errors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the contract error body from places where Spring MVC message converters
 * are not available (servlet filters, security entry points). The shape is tiny,
 * so it is rendered by hand, independent of the Jackson major version.
 */
public final class ErrorJson {

	private ErrorJson() {
	}

	public static String render(Map<String, List<String>> errors) {
		StringBuilder sb = new StringBuilder("{\"errors\":{");
		boolean firstKey = true;
		for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
			if (!firstKey) {
				sb.append(',');
			}
			firstKey = false;
			quote(sb, entry.getKey()).append(":[");
			boolean first = true;
			for (String message : entry.getValue()) {
				if (!first) {
					sb.append(',');
				}
				first = false;
				quote(sb, message);
			}
			sb.append(']');
		}
		return sb.append("}}").toString();
	}

	public static void write(HttpServletResponse response, int status, Map<String, List<String>> errors)
			throws IOException {
		if (response.isCommitted()) {
			return;
		}
		byte[] body = render(errors).getBytes(StandardCharsets.UTF_8);
		response.setStatus(status);
		response.setContentType("application/json");
		response.setContentLength(body.length);
		response.getOutputStream().write(body);
	}

	private static StringBuilder quote(StringBuilder sb, String value) {
		sb.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					}
					else {
						sb.append(c);
					}
				}
			}
		}
		return sb.append('"');
	}
}
