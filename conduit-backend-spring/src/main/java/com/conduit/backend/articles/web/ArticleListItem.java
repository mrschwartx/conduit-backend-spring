package com.conduit.backend.articles.web;

import java.util.List;

import com.conduit.backend.articles.application.ArticleView;
import com.conduit.backend.profiles.application.ProfileView;

/** Article as it appears in list endpoints: identical to Article but WITHOUT "body". */
public record ArticleListItem(
		String slug,
		String title,
		String description,
		List<String> tagList,
		String createdAt,
		String updatedAt,
		boolean favorited,
		long favoritesCount,
		ProfileView author) {

	public static ArticleListItem from(ArticleView a) {
		return new ArticleListItem(a.slug(), a.title(), a.description(), a.tagList(), a.createdAt(),
				a.updatedAt(), a.favorited(), a.favoritesCount(), a.author());
	}
}
