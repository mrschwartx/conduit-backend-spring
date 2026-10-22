package com.conduit.backend.comments.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.conduit.backend.comments.domain.Comment;
import com.conduit.backend.comments.persistence.CommentQueryRepository;
import com.conduit.backend.comments.persistence.CommentQueryRepository.Stored;
import com.conduit.backend.comments.persistence.CommentRepository;
import com.conduit.backend.shared.errors.ApiException;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.validation.FieldErrors;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.validation.Text;

@Service
public class CommentService {

	static final int BODY_MAX = 1000;

	private static final String ARTICLE = "article";
	private static final String COMMENT = "comment";

	private final CommentRepository comments;
	private final CommentQueryRepository queries;
	private final Clock clock;

	public CommentService(CommentRepository comments, CommentQueryRepository queries, Clock clock) {
		this.comments = comments;
		this.queries = queries;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<CommentView> list(String slug, AuthUser viewer) {
		long articleId = articleId(slug);
		return queries.findByArticle(articleId, AuthUser.idOrAnonymous(viewer));
	}

	@Transactional
	public CommentView add(AuthUser author, String slug, Fields fields) {
		long articleId = articleId(slug);

		FieldErrors errors = new FieldErrors();
		String body = body(fields.get("body"), errors);
		errors.throwUnprocessableIfAny();

		Comment saved = comments.saveAndFlush(new Comment(articleId, author.id(), body, now()));
		return queries.findById(saved.getId(), author.id())
				.orElseThrow(() -> ApiException.notFound(COMMENT))
				.view();
	}

	@Transactional
	public void delete(AuthUser user, String slug, String rawId) {
		long articleId = articleId(slug);
		long commentId = parseId(rawId);

		Stored stored = queries.findById(commentId, user.id())
				.filter(s -> s.articleId() == articleId)
				.orElseThrow(() -> ApiException.notFound(COMMENT));
		if (stored.authorId() != user.id()) {
			throw ApiException.forbidden(COMMENT);
		}
		comments.deleteById(commentId);
		comments.flush();
	}

	static String body(Object raw, FieldErrors errors) {
		String value = Text.required(raw, "body", errors);
		if (value == null) {
			return null;
		}
		return Text.max(value, BODY_MAX, "body", errors) ? value : null;
	}

	private long articleId(String slug) {
		return queries.findArticleId(slug).orElseThrow(() -> ApiException.notFound(ARTICLE));
	}

	private static long parseId(String raw) {
		try {
			long id = Long.parseLong(raw);
			if (id >= 1) {
				return id;
			}
		}
		catch (NumberFormatException ex) {
		}
		throw ApiException.of(ApiException.UNPROCESSABLE, "id", Text.INVALID);
	}

	private Instant now() {
		return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
	}
}
