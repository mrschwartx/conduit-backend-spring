package com.conduit.backend.comment.application.view;

import java.time.Instant;

public record CommentView(Long id, Instant createdAt, Instant updatedAt, String body, CommentAuthorView author) {
}
