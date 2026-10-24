package com.conduit.backend.article.domain;

import com.conduit.backend.shared.kernel.PageResult;
import com.conduit.backend.shared.kernel.Pagination;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Article save(Article article);

    void delete(Article article);

    PageResult<Article> search(ArticleSearchCriteria criteria, Pagination pagination);

    List<String> findAllTags();
}
