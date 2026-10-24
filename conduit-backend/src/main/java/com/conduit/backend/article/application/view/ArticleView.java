package com.conduit.backend.article.application.view;

import java.time.Instant;
import java.util.List;

public record ArticleView(
        String slug,
        String title,
        String description,
        String body,
        List<String> tagList,
        Instant createdAt,
        Instant updatedAt,
        boolean favorited,
        long favoritesCount,
        AuthorView author) {
}
