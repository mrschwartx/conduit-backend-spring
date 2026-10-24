package com.conduit.backend.article.presentation;

import com.conduit.backend.article.application.ArticleService;
import com.conduit.backend.article.application.command.CreateArticleCommand;
import com.conduit.backend.article.application.command.UpdateArticleCommand;
import com.conduit.backend.article.presentation.dto.ArticleRequests.NewArticleRequest;
import com.conduit.backend.article.presentation.dto.ArticleRequests.UpdateArticleRequest;
import com.conduit.backend.article.presentation.dto.ArticleResponses.MultipleArticlesResponse;
import com.conduit.backend.article.presentation.dto.ArticleResponses.SingleArticleResponse;
import com.conduit.backend.shared.kernel.Pagination;
import com.conduit.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles")
@Tag(name = "Articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/feed")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "GetArticlesFeed", summary = "Get recent articles from users you follow")
    public MultipleArticlesResponse feed(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @RequestParam(required = false) Integer offset,
                                         @RequestParam(required = false) Integer limit) {
        return MultipleArticlesResponse.from(
                articleService.feed(principal.userId(), Pagination.of(offset, limit)));
    }

    @GetMapping
    @Operation(operationId = "GetArticles", summary = "Get recent articles globally")
    public MultipleArticlesResponse list(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @RequestParam(required = false) String tag,
                                         @RequestParam(required = false) String author,
                                         @RequestParam(required = false) String favorited,
                                         @RequestParam(required = false) Integer offset,
                                         @RequestParam(required = false) Integer limit) {
        return MultipleArticlesResponse.from(articleService.list(
                viewerId(principal), tag, author, favorited, Pagination.of(offset, limit)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "CreateArticle", summary = "Create an article")
    public SingleArticleResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @Valid @RequestBody NewArticleRequest request) {
        return SingleArticleResponse.from(articleService.create(principal.userId(), new CreateArticleCommand(
                request.article().title(), request.article().description(),
                request.article().body(), request.article().tagList())));
    }

    @GetMapping("/{slug}")
    @Operation(operationId = "GetArticle", summary = "Get an article")
    public SingleArticleResponse get(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable String slug) {
        return SingleArticleResponse.from(articleService.get(viewerId(principal), slug));
    }

    @PutMapping("/{slug}")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "UpdateArticle", summary = "Update an article")
    public SingleArticleResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable String slug,
                                        @Valid @RequestBody UpdateArticleRequest request) {
        return SingleArticleResponse.from(articleService.update(principal.userId(), slug, new UpdateArticleCommand(
                request.article().title(), request.article().description(),
                request.article().body(), request.article().tagList())));
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "DeleteArticle", summary = "Delete an article")
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String slug) {
        articleService.delete(principal.userId(), slug);
    }

    @PostMapping("/{slug}/favorite")
    @SecurityRequirement(name = "Token")
    @Tag(name = "Favorites")
    @Operation(operationId = "CreateArticleFavorite", summary = "Favorite an article")
    public SingleArticleResponse favorite(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @PathVariable String slug) {
        return SingleArticleResponse.from(articleService.favorite(principal.userId(), slug));
    }

    @DeleteMapping("/{slug}/favorite")
    @SecurityRequirement(name = "Token")
    @Tag(name = "Favorites")
    @Operation(operationId = "DeleteArticleFavorite", summary = "Unfavorite an article")
    public SingleArticleResponse unfavorite(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable String slug) {
        return SingleArticleResponse.from(articleService.unfavorite(principal.userId(), slug));
    }

    private Long viewerId(AuthenticatedUser principal) {
        return principal == null ? null : principal.userId();
    }
}
