package com.conduit.backend.comments.web;

import com.conduit.backend.comments.application.CommentView;

public record CommentEnvelope(CommentView comment) {
}
