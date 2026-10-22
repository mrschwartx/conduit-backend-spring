package com.conduit.backend.articles.application;

import java.util.List;

public record ArticlePage(List<ArticleView> articles, long total) {
}
