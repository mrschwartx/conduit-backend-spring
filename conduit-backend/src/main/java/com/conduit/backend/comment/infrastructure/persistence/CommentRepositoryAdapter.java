package com.conduit.backend.comment.infrastructure.persistence;

import com.conduit.backend.comment.domain.Comment;
import com.conduit.backend.comment.domain.CommentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class CommentRepositoryAdapter implements CommentRepository {

    private final CommentJpaRepository repository;

    CommentRepositoryAdapter(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Comment save(Comment comment) {
        CommentEntity entity = new CommentEntity(comment.getArticleId(), comment.getAuthorId(), comment.getBody(),
                comment.getCreatedAt(), comment.getUpdatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Comment> findByArticleId(Long articleId) {
        return repository.findByArticleIdOrderByIdAsc(articleId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(Comment comment) {
        repository.deleteById(comment.getId());
    }

    @Override
    public void deleteAllByArticleId(Long articleId) {
        repository.deleteByArticleId(articleId);
    }

    private Comment toDomain(CommentEntity entity) {
        return Comment.restore(entity.getId(), entity.getArticleId(), entity.getAuthorId(), entity.getBody(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
