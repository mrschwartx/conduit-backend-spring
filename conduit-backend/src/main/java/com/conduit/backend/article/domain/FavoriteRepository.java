package com.conduit.backend.article.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FavoriteRepository {

    boolean exists(Long articleId, Long userId);

    void add(Long articleId, Long userId);

    void remove(Long articleId, Long userId);

    void removeAllByArticle(Long articleId);

    Map<Long, Long> countByArticleIds(Collection<Long> articleIds);

    Set<Long> findFavoritedArticleIds(Long userId, Collection<Long> articleIds);
}
