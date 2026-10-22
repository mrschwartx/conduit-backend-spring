package com.conduit.backend.articles.web;

import java.util.List;

public record ArticlesEnvelope(List<ArticleListItem> articles, long articlesCount) {
}
