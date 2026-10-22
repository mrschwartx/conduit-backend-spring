package com.conduit.backend.shared.errors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.conduit.backend.shared.web.BodyTooLargeException;

/**
 * The single place that turns exceptions into the contract error format.
 * Extends {@link ResponseEntityExceptionHandler} so that every Spring MVC
 * framework exception also ends up here, with its ProblemDetail body replaced.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
		return respond(ex.status(), ex.errors());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
		String detail = String.valueOf(rootMessage(ex)).toLowerCase();
		if (detail.contains("ux_users_username")) {
			return respond(ApiException.CONFLICT, Map.of("username", List.of("has already been taken")));
		}
		if (detail.contains("ux_users_email")) {
			return respond(ApiException.CONFLICT, Map.of("email", List.of("has already been taken")));
		}
		if (detail.contains("ux_articles_slug")) {
			return respond(ApiException.CONFLICT, Map.of("slug", List.of("has already been taken")));
		}
		log.error("Unexpected data integrity violation", ex);
		return respond(500, Map.of("server", List.of("internal server error")));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		// stack trace goes to the server log only, never to the client
		log.error("Unhandled exception", ex);
		return respond(500, Map.of("server", List.of("internal server error")));
	}

	/** Reached for every standard Spring MVC exception handled by the parent class. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		int status = statusCode.value();
		Map<String, List<String>> errors;
		if (ex instanceof HttpMessageNotReadableException) {
			if (hasCause(ex, BodyTooLargeException.class)) {
				status = 413;
				errors = Map.of("body", List.of("is too large"));
			}
			else {
				status = 400;
				errors = Map.of("body", List.of("is not valid JSON"));
			}
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			status = 415;
			errors = Map.of("body", List.of("content type must be application/json"));
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			status = 406;
			errors = Map.of("accept", List.of("must allow application/json"));
		}
		else if (ex instanceof HttpRequestMethodNotSupportedException) {
			status = 405;
			errors = Map.of("method", List.of("not allowed"));
		}
		else if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
			status = 404;
			errors = Map.of("path", List.of("not found"));
		}
		else if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
			status = ApiException.UNPROCESSABLE;
			errors = Map.of(mismatch.getName(), List.of("is invalid"));
		}
		else if (ex instanceof TypeMismatchException mismatch) {
			status = ApiException.UNPROCESSABLE;
			String name = mismatch.getPropertyName() != null ? mismatch.getPropertyName() : "request";
			errors = Map.of(name, List.of("is invalid"));
		}
		else if (ex instanceof MethodArgumentNotValidException invalid) {
			status = ApiException.UNPROCESSABLE;
			Map<String, List<String>> fields = new LinkedHashMap<>();
			invalid.getBindingResult().getFieldErrors().forEach(fe -> fields
					.computeIfAbsent(fe.getField(), k -> new java.util.ArrayList<>())
					.add(fe.getDefaultMessage() == null ? "is invalid" : fe.getDefaultMessage()));
			errors = fields.isEmpty() ? Map.of("request", List.of("is invalid")) : fields;
		}
		else if (status >= 500) {
			log.error("Unhandled framework exception", ex);
			errors = Map.of("server", List.of("internal server error"));
		}
		else {
			errors = Map.of("request", List.of("is invalid"));
		}
		HttpStatus resolved = HttpStatus.resolve(status);
		if (resolved == null) {
			status = 500;
		}
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ErrorResponse(errors));
	}

	private static ResponseEntity<ErrorResponse> respond(int status, Map<String, List<String>> errors) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ErrorResponse(errors));
	}

	private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
		for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
			if (type.isInstance(t)) {
				return true;
			}
		}
		return false;
	}

	private static String rootMessage(Throwable ex) {
		Throwable t = ex;
		while (t.getCause() != null && t.getCause() != t) {
			t = t.getCause();
		}
		return t.getMessage();
	}
}
