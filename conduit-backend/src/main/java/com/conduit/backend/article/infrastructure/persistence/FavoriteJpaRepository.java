package com.conduit.backend.article.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleId(Long articleId);

    @Query("select f.articleId, count(f) from FavoriteEntity f where f.articleId in :articleIds group by f.articleId")
    List<Object[]> countByArticleIds(Collection<Long> articleIds);

    @Query("select f.articleId from FavoriteEntity f where f.userId = :userId and f.articleId in :articleIds")
    List<Long> findFavoritedArticleIds(Long userId, Collection<Long> articleIds);
}
