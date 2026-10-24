package com.conduit.backend.comment.domain;

import java.time.Instant;

public final class Comment {

    private final Long id;
    private final Long articleId;
    private final Long authorId;
    private final String body;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Comment(Long id, Long articleId, Long authorId, String body, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.articleId = articleId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Comment create(Long articleId, Long authorId, String body, Instant now) {
        return new Comment(null, articleId, authorId, body, now, now);
    }

    public static Comment restore(Long id, Long articleId, Long authorId, String body,
                                  Instant createdAt, Instant updatedAt) {
        return new Comment(id, articleId, authorId, body, createdAt, updatedAt);
    }

    public boolean isAuthoredBy(Long userId) {
        return authorId.equals(userId);
    }

    public boolean belongsToArticle(Long candidateArticleId) {
        return articleId.equals(candidateArticleId);
    }

    public Long getId() {
        return id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
