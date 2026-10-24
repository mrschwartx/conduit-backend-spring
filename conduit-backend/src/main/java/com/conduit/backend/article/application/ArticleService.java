package com.conduit.backend.article.application;

import com.conduit.backend.article.application.command.CreateArticleCommand;
import com.conduit.backend.article.application.command.UpdateArticleCommand;
import com.conduit.backend.article.application.view.ArticleListView;
import com.conduit.backend.article.application.view.ArticleView;
import com.conduit.backend.article.contract.ArticleDeletedEvent;
import com.conduit.backend.article.domain.Article;
import com.conduit.backend.article.domain.ArticleRepository;
import com.conduit.backend.article.domain.ArticleSearchCriteria;
import com.conduit.backend.article.domain.FavoriteRepository;
import com.conduit.backend.article.domain.Slug;
import com.conduit.backend.identity.contract.UserDirectory;
import com.conduit.backend.identity.contract.UserSummary;
import com.conduit.backend.shared.kernel.ConflictException;
import com.conduit.backend.shared.kernel.ForbiddenException;
import com.conduit.backend.shared.kernel.NotFoundException;
import com.conduit.backend.shared.kernel.PageResult;
import com.conduit.backend.shared.kernel.Pagination;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArticleService {

    private static final String TAKEN = "has already been taken";

    private final ArticleRepository articleRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserDirectory userDirectory;
    private final ArticleAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ArticleService(ArticleRepository articleRepository, FavoriteRepository favoriteRepository,
                          UserDirectory userDirectory, ArticleAssembler assembler,
                          ApplicationEventPublisher eventPublisher, Clock clock) {
        this.articleRepository = articleRepository;
        this.favoriteRepository = favoriteRepository;
        this.userDirectory = userDirectory;
        this.assembler = assembler;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public ArticleView create(Long authorId, CreateArticleCommand command) {
        Article article = Article.create(command.title(), command.description(), command.body(),
                command.tagList(), authorId, clock.instant());
        if (articleRepository.existsBySlug(article.getSlug())) {
            throw new ConflictException("title", TAKEN);
        }
        return assembler.assemble(articleRepository.save(article), authorId);
    }

    @Transactional(readOnly = true)
    public ArticleView get(Long viewerId, String slug) {
        return assembler.assemble(findArticle(slug), viewerId);
    }

    public ArticleView update(Long userId, String slug, UpdateArticleCommand command) {
        Article article = findArticle(slug);
        if (!article.isAuthoredBy(userId)) {
            throw new ForbiddenException("article");
        }
        if (command.title() != null) {
            String newSlug = Slug.from(command.title()).value();
            if (!newSlug.equals(article.getSlug()) && articleRepository.existsBySlug(newSlug)) {
                throw new ConflictException("title", TAKEN);
            }
        }
        article.update(command.title(), command.description(), command.body(), command.tagList(), clock.instant());
        return assembler.assemble(articleRepository.save(article), userId);
    }

    public void delete(Long userId, String slug) {
        Article article = findArticle(slug);
        if (!article.isAuthoredBy(userId)) {
            throw new ForbiddenException("article");
        }
        favoriteRepository.removeAllByArticle(article.getId());
        articleRepository.delete(article);
        eventPublisher.publishEvent(new ArticleDeletedEvent(article.getId()));
    }

    @Transactional(readOnly = true)
    public ArticleListView list(Long viewerId, String tag, String author, String favorited, Pagination pagination) {
        Set<Long> authorIds = null;
        if (hasText(author)) {
            Optional<UserSummary> authorSummary = userDirectory.findByUsername(author);
            if (authorSummary.isEmpty()) {
                return new ArticleListView(List.of(), 0);
            }
            authorIds = Set.of(authorSummary.get().id());
        }
        Long favoritedBy = null;
        if (hasText(favorited)) {
            Optional<UserSummary> favoriter = userDirectory.findByUsername(favorited);
            if (favoriter.isEmpty()) {
                return new ArticleListView(List.of(), 0);
            }
            favoritedBy = favoriter.get().id();
        }
        ArticleSearchCriteria criteria = new ArticleSearchCriteria(hasText(tag) ? tag : null, authorIds, favoritedBy);
        return toListView(articleRepository.search(criteria, pagination), viewerId);
    }

    @Transactional(readOnly = true)
    public ArticleListView feed(Long userId, Pagination pagination) {
        Set<Long> followed = userDirectory.findFollowedIds(userId);
        if (followed.isEmpty()) {
            return new ArticleListView(List.of(), 0);
        }
        ArticleSearchCriteria criteria = new ArticleSearchCriteria(null, followed, null);
        return toListView(articleRepository.search(criteria, pagination), userId);
    }

    public ArticleView favorite(Long userId, String slug) {
        Article article = findArticle(slug);
        if (!favoriteRepository.exists(article.getId(), userId)) {
            favoriteRepository.add(article.getId(), userId);
        }
        return assembler.assemble(article, userId);
    }

    public ArticleView unfavorite(Long userId, String slug) {
        Article article = findArticle(slug);
        if (favoriteRepository.exists(article.getId(), userId)) {
            favoriteRepository.remove(article.getId(), userId);
        }
        return assembler.assemble(article, userId);
    }

    @Transactional(readOnly = true)
    public List<String> tags() {
        return articleRepository.findAllTags();
    }

    private Article findArticle(String slug) {
        return articleRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException("article"));
    }

    private ArticleListView toListView(PageResult<Article> page, Long viewerId) {
        return new ArticleListView(assembler.assemble(page.items(), viewerId), page.total());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
