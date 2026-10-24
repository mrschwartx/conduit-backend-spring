package com.conduit.backend.article.infrastructure.persistence;

import com.conduit.backend.article.domain.Article;
import com.conduit.backend.article.domain.ArticleRepository;
import com.conduit.backend.article.domain.ArticleSearchCriteria;
import com.conduit.backend.shared.kernel.PageResult;
import com.conduit.backend.shared.kernel.Pagination;
import com.conduit.backend.shared.persistence.OffsetPageRequest;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class ArticleRepositoryAdapter implements ArticleRepository {

    private final ArticleJpaRepository repository;

    ArticleRepositoryAdapter(ArticleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Article> findBySlug(String slug) {
        return repository.findBySlug(slug).map(this::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = article.getId() == null
                ? ArticleEntity.create()
                : repository.findById(article.getId()).orElseGet(ArticleEntity::create);
        entity.setSlug(article.getSlug());
        entity.setTitle(article.getTitle());
        entity.setDescription(article.getDescription());
        entity.setBody(article.getBody());
        entity.setAuthorId(article.getAuthorId());
        entity.setCreatedAt(article.getCreatedAt());
        entity.setUpdatedAt(article.getUpdatedAt());
        entity.getTags().clear();
        entity.getTags().addAll(article.getTags());
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(Article article) {
        repository.deleteById(article.getId());
    }

    @Override
    public PageResult<Article> search(ArticleSearchCriteria criteria, Pagination pagination) {
        Specification<ArticleEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.tag() != null) {
                predicates.add(builder.isMember(criteria.tag(), root.<List<String>>get("tags")));
            }
            if (criteria.authorIds() != null) {
                predicates.add(root.get("authorId").in(criteria.authorIds()));
            }
            if (criteria.favoritedByUserId() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<FavoriteEntity> favorite = subquery.from(FavoriteEntity.class);
                subquery.select(favorite.get("articleId"))
                        .where(builder.equal(favorite.get("userId"), criteria.favoritedByUserId()));
                predicates.add(root.get("id").in(subquery));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<ArticleEntity> page = repository.findAll(specification,
                new OffsetPageRequest(pagination.offset(), pagination.limit(), sort));
        return new PageResult<>(page.getContent().stream().map(this::toDomain).toList(), page.getTotalElements());
    }

    @Override
    public List<String> findAllTags() {
        return repository.findAllTags();
    }

    private Article toDomain(ArticleEntity entity) {
        return Article.restore(entity.getId(), entity.getSlug(), entity.getTitle(), entity.getDescription(),
                entity.getBody(), entity.getAuthorId(), entity.getTags(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
