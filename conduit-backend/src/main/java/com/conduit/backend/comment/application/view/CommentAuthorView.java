package com.conduit.backend.comment.application.view;

public record CommentAuthorView(String username, String bio, String image, boolean following) {
}
