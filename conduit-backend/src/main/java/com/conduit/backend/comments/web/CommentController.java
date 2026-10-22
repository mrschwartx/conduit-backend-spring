package com.conduit.backend.comments.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conduit.backend.comments.application.CommentService;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.web.Api;

@RestController
@RequestMapping(Api.V1 + "/articles/{slug}/comments")
public class CommentController {

	private final CommentService comments;

	public CommentController(CommentService comments) {
		this.comments = comments;
	}

	@GetMapping
	public CommentsEnvelope list(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser viewer) {
		return new CommentsEnvelope(comments.list(slug, viewer));
	}

	@PostMapping
	public ResponseEntity<CommentEnvelope> add(@PathVariable("slug") String slug,
			@AuthenticationPrincipal AuthUser user, @RequestBody Map<String, Object> body) {
		return ResponseEntity.status(201)
				.body(new CommentEnvelope(comments.add(user, slug, Fields.envelope(body, "comment"))));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("slug") String slug, @PathVariable("id") String id,
			@AuthenticationPrincipal AuthUser user) {
		comments.delete(user, slug, id);
		return ResponseEntity.noContent().build();
	}
}
