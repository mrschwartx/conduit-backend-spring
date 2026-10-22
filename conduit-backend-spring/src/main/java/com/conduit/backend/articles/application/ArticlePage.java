package com.conduit.backend.articles.application;

import java.util.List;

/** One page of articles plus the total number of matches (ignoring offset/limit). */
public record ArticlePage(List<ArticleView> articles, long total) {
}
