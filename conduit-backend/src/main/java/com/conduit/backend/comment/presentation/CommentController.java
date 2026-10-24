package com.conduit.backend.comment.presentation;

import com.conduit.backend.comment.application.CommentService;
import com.conduit.backend.comment.presentation.dto.CommentRequests.NewCommentRequest;
import com.conduit.backend.comment.presentation.dto.CommentResponses.MultipleCommentsResponse;
import com.conduit.backend.comment.presentation.dto.CommentResponses.SingleCommentResponse;
import com.conduit.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles/{slug}/comments")
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(operationId = "GetArticleComments", summary = "Get comments for an article")
    public MultipleCommentsResponse list(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable String slug) {
        Long viewerId = principal == null ? null : principal.userId();
        return MultipleCommentsResponse.from(commentService.list(viewerId, slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "CreateArticleComment", summary = "Create a comment for an article")
    public SingleCommentResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable String slug,
                                        @Valid @RequestBody NewCommentRequest request) {
        return SingleCommentResponse.from(
                commentService.add(principal.userId(), slug, request.comment().body()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "DeleteArticleComment", summary = "Delete a comment for an article")
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal,
                       @PathVariable String slug,
                       @PathVariable Long id) {
        commentService.delete(principal.userId(), slug, id);
    }
}
