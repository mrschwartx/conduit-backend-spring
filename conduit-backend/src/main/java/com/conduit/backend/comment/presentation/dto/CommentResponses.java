package com.conduit.backend.comment.presentation.dto;

import com.conduit.backend.comment.application.view.CommentAuthorView;
import com.conduit.backend.comment.application.view.CommentView;
import java.time.Instant;
import java.util.List;

public final class CommentResponses {

    private CommentResponses() {
    }

    public record AuthorBody(String username, String bio, String image, boolean following) {

        static AuthorBody from(CommentAuthorView view) {
            return new AuthorBody(view.username(), view.bio(), view.image(), view.following());
        }
    }

    public record CommentBody(Long id, Instant createdAt, Instant updatedAt, String body, AuthorBody author) {

        static CommentBody from(CommentView view) {
            return new CommentBody(view.id(), view.createdAt(), view.updatedAt(), view.body(),
                    AuthorBody.from(view.author()));
        }
    }

    public record SingleCommentResponse(CommentBody comment) {

        public static SingleCommentResponse from(CommentView view) {
            return new SingleCommentResponse(CommentBody.from(view));
        }
    }

    public record MultipleCommentsResponse(List<CommentBody> comments) {

        public static MultipleCommentsResponse from(List<CommentView> views) {
            return new MultipleCommentsResponse(views.stream().map(CommentBody::from).toList());
        }
    }
}
