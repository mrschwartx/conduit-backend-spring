package com.conduit.backend.article.infrastructure.persistence;

import com.conduit.backend.article.domain.FavoriteRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class FavoriteRepositoryAdapter implements FavoriteRepository {

    private final FavoriteJpaRepository repository;

    FavoriteRepositoryAdapter(FavoriteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(Long articleId, Long userId) {
        return repository.existsByArticleIdAndUserId(articleId, userId);
    }

    @Override
    public void add(Long articleId, Long userId) {
        repository.save(new FavoriteEntity(articleId, userId));
    }

    @Override
    public void remove(Long articleId, Long userId) {
        repository.deleteByArticleIdAndUserId(articleId, userId);
    }

    @Override
    public void removeAllByArticle(Long articleId) {
        repository.deleteByArticleId(articleId);
    }

    @Override
    public Map<Long, Long> countByArticleIds(Collection<Long> articleIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (articleIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : repository.countByArticleIds(articleIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public Set<Long> findFavoritedArticleIds(Long userId, Collection<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(repository.findFavoritedArticleIds(userId, articleIds));
    }
}
