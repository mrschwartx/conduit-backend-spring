package com.conduit.backend.articles.application;

import java.util.List;

import com.conduit.backend.profiles.application.ProfileView;

public record ArticleView(
		String slug,
		String title,
		String description,
		String body,
		List<String> tagList,
		String createdAt,
		String updatedAt,
		boolean favorited,
		long favoritesCount,
		ProfileView author) {
}
