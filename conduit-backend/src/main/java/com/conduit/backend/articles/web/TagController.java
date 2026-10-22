package com.conduit.backend.articles.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conduit.backend.articles.application.ArticleService;
import com.conduit.backend.shared.web.Api;

@RestController
@RequestMapping(Api.V1 + "/tags")
public class TagController {

	private final ArticleService articles;

	public TagController(ArticleService articles) {
		this.articles = articles;
	}

	@GetMapping
	public TagsEnvelope tags() {
		return new TagsEnvelope(articles.popularTags());
	}
}
