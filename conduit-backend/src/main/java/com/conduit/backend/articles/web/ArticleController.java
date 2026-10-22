package com.conduit.backend.articles.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.conduit.backend.articles.application.ArticleFilter;
import com.conduit.backend.articles.application.ArticlePage;
import com.conduit.backend.articles.application.ArticleService;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.validation.PageParams;
import com.conduit.backend.shared.web.Api;

@RestController
@RequestMapping(Api.V1 + "/articles")
public class ArticleController {

	private final ArticleService articles;

	public ArticleController(ArticleService articles) {
		this.articles = articles;
	}

	@GetMapping
	public ArticlesEnvelope list(
			@RequestParam(name = "tag", required = false) String tag,
			@RequestParam(name = "author", required = false) String author,
			@RequestParam(name = "favorited", required = false) String favorited,
			@RequestParam(name = "offset", required = false) String offset,
			@RequestParam(name = "limit", required = false) String limit,
			@AuthenticationPrincipal AuthUser viewer) {
		PageParams page = PageParams.parse(offset, limit);
		return envelope(articles.list(new ArticleFilter(tag, author, favorited), page, viewer));
	}

	@GetMapping("/feed")
	public ArticlesEnvelope feed(
			@RequestParam(name = "offset", required = false) String offset,
			@RequestParam(name = "limit", required = false) String limit,
			@AuthenticationPrincipal AuthUser viewer) {
		return envelope(articles.feed(PageParams.parse(offset, limit), viewer));
	}

	@GetMapping("/{slug}")
	public ArticleEnvelope get(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser viewer) {
		return new ArticleEnvelope(articles.get(slug, viewer));
	}

	@PostMapping
	public ResponseEntity<ArticleEnvelope> create(@AuthenticationPrincipal AuthUser user,
			@RequestBody Map<String, Object> body) {
		return ResponseEntity.status(201)
				.body(new ArticleEnvelope(articles.create(user, Fields.envelope(body, "article"))));
	}

	@PutMapping("/{slug}")
	public ArticleEnvelope update(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser user,
			@RequestBody Map<String, Object> body) {
		return new ArticleEnvelope(articles.update(user, slug, Fields.envelope(body, "article")));
	}

	@DeleteMapping("/{slug}")
	public ResponseEntity<Void> delete(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser user) {
		articles.delete(user, slug);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{slug}/favorite")
	public ArticleEnvelope favorite(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser user) {
		return new ArticleEnvelope(articles.setFavorite(user, slug, true));
	}

	@DeleteMapping("/{slug}/favorite")
	public ArticleEnvelope unfavorite(@PathVariable("slug") String slug, @AuthenticationPrincipal AuthUser user) {
		return new ArticleEnvelope(articles.setFavorite(user, slug, false));
	}

	private static ArticlesEnvelope envelope(ArticlePage page) {
		return new ArticlesEnvelope(page.articles().stream().map(ArticleListItem::from).toList(), page.total());
	}
}
