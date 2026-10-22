package com.conduit.backend.shared.errors;

import java.util.List;
import java.util.Map;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Replaces Spring Boot's default /error handling (used for errors raised outside
 * the MVC layer, e.g. malformed URLs rejected by the servlet container) so that
 * no non-contract JSON can escape.
 */
@RestController
public class ContractErrorController implements ErrorController {

	@RequestMapping("/error")
	public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
		Object attr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		int status = attr instanceof Integer i ? i : 500;
		Map<String, List<String>> errors = switch (status) {
			case 400 -> Map.of("request", List.of("is invalid"));
			case 404 -> Map.of("path", List.of("not found"));
			case 405 -> Map.of("method", List.of("not allowed"));
			case 413 -> Map.of("body", List.of("is too large"));
			default -> status >= 500 ? Map.of("server", List.of("internal server error"))
					: Map.of("request", List.of("is invalid"));
		};
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ErrorResponse(errors));
	}
}
