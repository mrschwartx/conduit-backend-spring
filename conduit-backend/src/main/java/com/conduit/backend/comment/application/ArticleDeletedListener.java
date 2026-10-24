package com.conduit.backend.comment.application;

import com.conduit.backend.article.contract.ArticleDeletedEvent;
import com.conduit.backend.comment.domain.CommentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ArticleDeletedListener {

    private final CommentRepository commentRepository;

    ArticleDeletedListener(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @EventListener
    void on(ArticleDeletedEvent event) {
        commentRepository.deleteAllByArticleId(event.articleId());
    }
}
