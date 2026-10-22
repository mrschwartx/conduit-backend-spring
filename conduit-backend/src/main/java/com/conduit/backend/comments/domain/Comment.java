package com.conduit.backend.comments.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "article_id", nullable = false)
	private Long articleId;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(nullable = false)
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Comment() {
	}

	public Comment(long articleId, long authorId, String body, Instant now) {
		this.articleId = articleId;
		this.authorId = authorId;
		this.body = body;
		this.createdAt = now;
		this.updatedAt = now;
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
}
