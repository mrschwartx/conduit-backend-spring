package com.conduit.backend.comments.application;

import com.conduit.backend.profiles.application.ProfileView;

/** Contract "Comment" object. */
public record CommentView(long id, String createdAt, String updatedAt, String body, ProfileView author) {
}
