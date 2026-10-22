package com.conduit.backend.articles.web;

import com.conduit.backend.articles.application.ArticleView;

/** Response envelope {"article": {...}}. */
public record ArticleEnvelope(ArticleView article) {
}
