package com.conduit.backend.articles.web;

import java.util.List;

/** Response envelope {"articles": [...], "articlesCount": n}. */
public record ArticlesEnvelope(List<ArticleListItem> articles, long articlesCount) {
}
