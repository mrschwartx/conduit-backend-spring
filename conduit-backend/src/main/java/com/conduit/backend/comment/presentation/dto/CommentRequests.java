package com.conduit.backend.comment.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CommentRequests {

    private CommentRequests() {
    }

    public record NewCommentRequest(@NotNull(message = "can't be blank") @Valid NewCommentBody comment) {
    }

    public record NewCommentBody(@NotBlank(message = "can't be blank") String body) {
    }
}
