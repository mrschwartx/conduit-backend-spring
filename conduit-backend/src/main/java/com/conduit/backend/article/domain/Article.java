package com.conduit.backend.article.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Article {

    private final Long id;
    private String slug;
    private String title;
    private String description;
    private String body;
    private final Long authorId;
    private List<String> tags;
    private final Instant createdAt;
    private Instant updatedAt;

    private Article(Long id, String slug, String title, String description, String body, Long authorId,
                    List<String> tags, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.body = body;
        this.authorId = authorId;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Article create(String title, String description, String body, Collection<String> tags,
                                 Long authorId, Instant now) {
        return new Article(null, Slug.from(title).value(), title, description, body, authorId,
                normalizeTags(tags), now, now);
    }

    public static Article restore(Long id, String slug, String title, String description, String body,
                                  Long authorId, List<String> tags, Instant createdAt, Instant updatedAt) {
        return new Article(id, slug, title, description, body, authorId, new ArrayList<>(tags), createdAt, updatedAt);
    }

    public void update(String title, String description, String body, Collection<String> tags, Instant now) {
        if (title != null) {
            this.title = title;
            this.slug = Slug.from(title).value();
        }
        if (description != null) {
            this.description = description;
        }
        if (body != null) {
            this.body = body;
        }
        if (tags != null) {
            this.tags = normalizeTags(tags);
        }
        this.updatedAt = now;
    }

    public boolean isAuthoredBy(Long userId) {
        return authorId.equals(userId);
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBody() {
        return body;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
