package com.conduit.backend.article.application;

import com.conduit.backend.article.contract.ArticleLookup;
import com.conduit.backend.article.domain.Article;
import com.conduit.backend.article.domain.ArticleRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ArticleLookupService implements ArticleLookup {

    private final ArticleRepository articleRepository;

    ArticleLookupService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public Optional<Long> findIdBySlug(String slug) {
        return articleRepository.findBySlug(slug).map(Article::getId);
    }
}
