package com.conduit.backend.comment.application;

import com.conduit.backend.article.contract.ArticleLookup;
import com.conduit.backend.comment.application.view.CommentAuthorView;
import com.conduit.backend.comment.application.view.CommentView;
import com.conduit.backend.comment.domain.Comment;
import com.conduit.backend.comment.domain.CommentRepository;
import com.conduit.backend.identity.contract.UserDirectory;
import com.conduit.backend.identity.contract.UserSummary;
import com.conduit.backend.shared.kernel.ForbiddenException;
import com.conduit.backend.shared.kernel.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleLookup articleLookup;
    private final UserDirectory userDirectory;
    private final Clock clock;

    public CommentService(CommentRepository commentRepository, ArticleLookup articleLookup,
                          UserDirectory userDirectory, Clock clock) {
        this.commentRepository = commentRepository;
        this.articleLookup = articleLookup;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    public CommentView add(Long userId, String slug, String body) {
        Long articleId = findArticleId(slug);
        Comment saved = commentRepository.save(Comment.create(articleId, userId, body, clock.instant()));
        return assemble(List.of(saved), userId).get(0);
    }

    @Transactional(readOnly = true)
    public List<CommentView> list(Long viewerId, String slug) {
        Long articleId = findArticleId(slug);
        return assemble(commentRepository.findByArticleId(articleId), viewerId);
    }

    public void delete(Long userId, String slug, Long commentId) {
        Long articleId = findArticleId(slug);
        Comment comment = commentRepository.findById(commentId)
                .filter(candidate -> candidate.belongsToArticle(articleId))
                .orElseThrow(() -> new NotFoundException("comment"));
        if (!comment.isAuthoredBy(userId)) {
            throw new ForbiddenException("comment");
        }
        commentRepository.delete(comment);
    }

    private Long findArticleId(String slug) {
        return articleLookup.findIdBySlug(slug).orElseThrow(() -> new NotFoundException("article"));
    }

    private List<CommentView> assemble(List<Comment> comments, Long viewerId) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        Map<Long, UserSummary> authors = userDirectory.findAllByIds(authorIds);
        Set<Long> followed = viewerId == null ? Set.of() : userDirectory.findFollowedIds(viewerId);
        return comments.stream().map(comment -> {
            UserSummary author = authors.get(comment.getAuthorId());
            if (author == null) {
                throw new IllegalStateException("author missing for comment " + comment.getId());
            }
            return new CommentView(comment.getId(), comment.getCreatedAt(), comment.getUpdatedAt(), comment.getBody(),
                    new CommentAuthorView(author.username(), author.bio(), author.image(),
                            followed.contains(author.id())));
        }).toList();
    }
}
