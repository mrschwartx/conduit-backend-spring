package com.conduit.backend.comment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByArticleIdOrderByIdAsc(Long articleId);

    void deleteByArticleId(Long articleId);
}
