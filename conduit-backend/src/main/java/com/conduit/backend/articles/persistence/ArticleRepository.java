package com.conduit.backend.articles.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.conduit.backend.articles.domain.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	Optional<Article> findBySlug(String slug);

	@Query("select a.id from Article a where a.slug = :slug")
	Optional<Long> findIdBySlug(@Param("slug") String slug);
}
