package com.conduit.backend.article.application;

import com.conduit.backend.article.application.view.ArticleView;
import com.conduit.backend.article.application.view.AuthorView;
import com.conduit.backend.article.domain.Article;
import com.conduit.backend.article.domain.FavoriteRepository;
import com.conduit.backend.identity.contract.UserDirectory;
import com.conduit.backend.identity.contract.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class ArticleAssembler {

    private final UserDirectory userDirectory;
    private final FavoriteRepository favoriteRepository;

    ArticleAssembler(UserDirectory userDirectory, FavoriteRepository favoriteRepository) {
        this.userDirectory = userDirectory;
        this.favoriteRepository = favoriteRepository;
    }

    ArticleView assemble(Article article, Long viewerId) {
        return assemble(List.of(article), viewerId).get(0);
    }

    List<ArticleView> assemble(List<Article> articles, Long viewerId) {
        if (articles.isEmpty()) {
            return List.of();
        }
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        List<Long> authorIds = articles.stream().map(Article::getAuthorId).distinct().toList();
        Map<Long, UserSummary> authors = userDirectory.findAllByIds(authorIds);
        Set<Long> followed = viewerId == null ? Set.of() : userDirectory.findFollowedIds(viewerId);
        Map<Long, Long> counts = favoriteRepository.countByArticleIds(articleIds);
        Set<Long> favorited = viewerId == null
                ? Set.of()
                : favoriteRepository.findFavoritedArticleIds(viewerId, articleIds);
        return articles.stream().map(article -> {
            UserSummary author = authors.get(article.getAuthorId());
            if (author == null) {
                throw new IllegalStateException("author missing for article " + article.getId());
            }
            return new ArticleView(
                    article.getSlug(),
                    article.getTitle(),
                    article.getDescription(),
                    article.getBody(),
                    article.getTags(),
                    article.getCreatedAt(),
                    article.getUpdatedAt(),
                    favorited.contains(article.getId()),
                    counts.getOrDefault(article.getId(), 0L),
                    new AuthorView(author.username(), author.bio(), author.image(), followed.contains(author.id())));
        }).toList();
    }
}
