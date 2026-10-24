package com.conduit.backend.article.presentation.dto;

import com.conduit.backend.article.application.view.ArticleListView;
import com.conduit.backend.article.application.view.ArticleView;
import com.conduit.backend.article.application.view.AuthorView;
import java.time.Instant;
import java.util.List;

public final class ArticleResponses {

    private ArticleResponses() {
    }

    public record AuthorBody(String username, String bio, String image, boolean following) {

        static AuthorBody from(AuthorView view) {
            return new AuthorBody(view.username(), view.bio(), view.image(), view.following());
        }
    }

    public record ArticleBody(
            String slug,
            String title,
            String description,
            String body,
            List<String> tagList,
            Instant createdAt,
            Instant updatedAt,
            boolean favorited,
            long favoritesCount,
            AuthorBody author) {

        static ArticleBody from(ArticleView view) {
            return new ArticleBody(view.slug(), view.title(), view.description(), view.body(), view.tagList(),
                    view.createdAt(), view.updatedAt(), view.favorited(), view.favoritesCount(),
                    AuthorBody.from(view.author()));
        }
    }

    public record ArticleSummaryBody(
            String slug,
            String title,
            String description,
            List<String> tagList,
            Instant createdAt,
            Instant updatedAt,
            boolean favorited,
            long favoritesCount,
            AuthorBody author) {

        static ArticleSummaryBody from(ArticleView view) {
            return new ArticleSummaryBody(view.slug(), view.title(), view.description(), view.tagList(),
                    view.createdAt(), view.updatedAt(), view.favorited(), view.favoritesCount(),
                    AuthorBody.from(view.author()));
        }
    }

    public record SingleArticleResponse(ArticleBody article) {

        public static SingleArticleResponse from(ArticleView view) {
            return new SingleArticleResponse(ArticleBody.from(view));
        }
    }

    public record MultipleArticlesResponse(List<ArticleSummaryBody> articles, long articlesCount) {

        public static MultipleArticlesResponse from(ArticleListView view) {
            return new MultipleArticlesResponse(
                    view.articles().stream().map(ArticleSummaryBody::from).toList(), view.articlesCount());
        }
    }

    public record TagsResponse(List<String> tags) {
    }
}
