package com.conduit.backend.comments.web;

import java.util.List;

import com.conduit.backend.comments.application.CommentView;

public record CommentsEnvelope(List<CommentView> comments) {
}
