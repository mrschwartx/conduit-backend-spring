package com.conduit.backend.article.application.view;

import java.util.List;

public record ArticleListView(List<ArticleView> articles, long articlesCount) {
}
