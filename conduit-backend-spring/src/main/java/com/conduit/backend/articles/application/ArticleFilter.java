package com.conduit.backend.articles.application;

/** Optional filters of GET /articles; all given filters combine with AND. */
public record ArticleFilter(String tag, String author, String favorited) {
}
