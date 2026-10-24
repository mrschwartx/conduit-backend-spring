package com.conduit.backend.article.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface ArticleJpaRepository extends JpaRepository<ArticleEntity, Long>, JpaSpecificationExecutor<ArticleEntity> {

    Optional<ArticleEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select distinct t from ArticleEntity a join a.tags t order by t")
    List<String> findAllTags();
}
