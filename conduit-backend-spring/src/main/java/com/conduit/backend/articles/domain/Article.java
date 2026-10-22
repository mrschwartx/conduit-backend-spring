package com.conduit.backend.articles.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String slug;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private String body;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Tags in first-seen order (persisted through the tag_position order column). */
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
	@OrderColumn(name = "tag_position")
	@Column(name = "tag", nullable = false)
	private List<String> tags = new ArrayList<>();

	protected Article() {
		// for JPA
	}

	public Article(String slug, String title, String description, String body, long authorId,
			List<String> tags, Instant now) {
		this.slug = slug;
		this.title = title;
		this.description = description;
		this.body = body;
		this.authorId = authorId;
		this.tags = new ArrayList<>(tags);
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Long getAuthorId() {
		return authorId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void replaceTags(List<String> newTags) {
		tags.clear();
		tags.addAll(newTags);
	}
}
