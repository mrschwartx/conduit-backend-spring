package com.conduit.backend.articles.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.conduit.backend.articles.domain.Article;
import com.conduit.backend.articles.persistence.ArticleQueryRepository;
import com.conduit.backend.articles.persistence.ArticleQueryRepository.Filter;
import com.conduit.backend.articles.persistence.ArticleRepository;
import com.conduit.backend.shared.errors.ApiException;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.validation.FieldErrors;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.validation.PageParams;

/**
 * Article use cases. Check order for protected operations:
 * authentication (filter) -> existence (404) -> ownership (403) -> validation (422).
 */
@Service
public class ArticleService {

	private static final String ARTICLE = "article";
	private static final int POPULAR_TAGS = 20;

	private final ArticleRepository articles;
	private final ArticleQueryRepository queries;
	private final Clock clock;

	public ArticleService(ArticleRepository articles, ArticleQueryRepository queries, Clock clock) {
		this.articles = articles;
		this.queries = queries;
		this.clock = clock;
	}

	/** {@code viewer} is null for anonymous callers. */
	@Transactional(readOnly = true)
	public ArticlePage list(ArticleFilter filter, PageParams page, AuthUser viewer) {
		Filter query = new Filter(null, filter.tag(), filter.author(), filter.favorited(), false);
		return queries.findPage(query, AuthUser.idOrAnonymous(viewer), page.offset(), page.limit());
	}

	@Transactional(readOnly = true)
	public ArticlePage feed(PageParams page, AuthUser viewer) {
		Filter query = new Filter(null, null, null, null, true);
		return queries.findPage(query, viewer.id(), page.offset(), page.limit());
	}

	@Transactional(readOnly = true)
	public ArticleView get(String slug, AuthUser viewer) {
		return queries.findBySlug(slug, AuthUser.idOrAnonymous(viewer))
				.orElseThrow(() -> ApiException.notFound(ARTICLE));
	}

	@Transactional
	public ArticleView create(AuthUser author, Fields fields) {
		FieldErrors errors = new FieldErrors();
		String title = ArticleRules.title(fields.get("title"), errors);
		String description = ArticleRules.description(fields.get("description"), errors);
		String body = ArticleRules.body(fields.get("body"), errors);
		List<String> tags = ArticleRules.tags(fields.get("tagList"), errors);
		errors.throwUnprocessableIfAny();

		String slug = allocateSlug(title, 0L);
		articles.saveAndFlush(new Article(slug, title, description, body, author.id(), tags, now()));
		return view(slug, author);
	}

	@Transactional
	public ArticleView update(AuthUser user, String slug, Fields fields) {
		Article article = articles.findBySlug(slug).orElseThrow(() -> ApiException.notFound(ARTICLE));
		if (article.getAuthorId() != user.id()) {
			throw ApiException.forbidden(ARTICLE);
		}

		FieldErrors errors = new FieldErrors();
		String title = fields.has("title") ? ArticleRules.title(fields.get("title"), errors) : null;
		String description = fields.has("description")
				? ArticleRules.description(fields.get("description"), errors) : null;
		String body = fields.has("body") ? ArticleRules.body(fields.get("body"), errors) : null;
		List<String> tags = fields.has("tagList") ? ArticleRules.tags(fields.get("tagList"), errors) : null;
		errors.throwUnprocessableIfAny();

		boolean changed = false;
		if (title != null) {
			if (!title.equals(article.getTitle())) {
				article.setTitle(title);
				// a changed title regenerates the slug; the article's own current slug does not count as taken
				article.setSlug(allocateSlug(title, article.getId()));
			}
			changed = true;
		}
		if (description != null) {
			article.setDescription(description);
			changed = true;
		}
		if (body != null) {
			article.setBody(body);
			changed = true;
		}
		if (tags != null) {
			article.replaceTags(tags);
			changed = true;
		}
		if (changed) {
			article.setUpdatedAt(now());
			articles.saveAndFlush(article);
		}
		return view(article.getSlug(), user);
	}

	@Transactional
	public void delete(AuthUser user, String slug) {
		Article article = articles.findBySlug(slug).orElseThrow(() -> ApiException.notFound(ARTICLE));
		if (article.getAuthorId() != user.id()) {
			throw ApiException.forbidden(ARTICLE);
		}
		// comments, favorites and tags are removed by ON DELETE CASCADE
		articles.delete(article);
		articles.flush();
	}

	/** Idempotent: favoriting twice (or unfavoriting a non-favorite) is not an error. */
	@Transactional
	public ArticleView setFavorite(AuthUser user, String slug, boolean favorite) {
		long articleId = articles.findIdBySlug(slug).orElseThrow(() -> ApiException.notFound(ARTICLE));
		if (favorite) {
			queries.addFavorite(user.id(), articleId);
		}
		else {
			queries.removeFavorite(user.id(), articleId);
		}
		return view(slug, user);
	}

	@Transactional(readOnly = true)
	public List<String> popularTags() {
		return queries.popularTags(POPULAR_TAGS);
	}

	private ArticleView view(String slug, AuthUser viewer) {
		return queries.findBySlug(slug, viewer.id()).orElseThrow(() -> ApiException.notFound(ARTICLE));
	}

	private String allocateSlug(String title, long excludeArticleId) {
		String base = SlugGenerator.slugify(title);
		return SlugGenerator.firstFree(base, queries.findTakenSlugs(base, excludeArticleId));
	}

	private Instant now() {
		return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
	}
}
