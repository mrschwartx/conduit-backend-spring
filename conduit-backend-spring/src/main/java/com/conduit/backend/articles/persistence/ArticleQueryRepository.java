package com.conduit.backend.articles.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.conduit.backend.articles.application.ArticlePage;
import com.conduit.backend.articles.application.ArticleView;
import com.conduit.backend.profiles.application.ProfileView;
import com.conduit.backend.shared.web.Timestamps;

/**
 * Read side for articles: filters, ordering, pagination, tag ranking, favorites
 * and the per-caller flags (favorited, following) are all computed in SQL.
 *
 * Query count per page is constant: one for the page (author, favoritesCount,
 * favorited and following come from joins/subqueries), one for the total, and one
 * for all tags of the page ("in (:ids)") - no N+1.
 */
@Repository
public class ArticleQueryRepository {

	/** Search criteria; null fields are not applied. */
	public record Filter(String slug, String tag, String author, String favoritedBy, boolean feedOnly) {

		public static Filter all() {
			return new Filter(null, null, null, null, false);
		}

		public static Filter ofSlug(String slug) {
			return new Filter(slug, null, null, null, false);
		}
	}

	private record Row(long id, String slug, String title, String description, String body,
			OffsetDateTime createdAt, OffsetDateTime updatedAt, String authorUsername, String authorBio,
			String authorImage, long favoritesCount, boolean favorited, boolean following) {
	}

	private record TagRow(long articleId, String tag) {
	}

	private static final RowMapper<Row> ROW_MAPPER = (rs, n) -> new Row(
			rs.getLong("id"),
			rs.getString("slug"),
			rs.getString("title"),
			rs.getString("description"),
			rs.getString("body"),
			rs.getObject("created_at", OffsetDateTime.class),
			rs.getObject("updated_at", OffsetDateTime.class),
			rs.getString("author_username"),
			rs.getString("author_bio"),
			rs.getString("author_image"),
			rs.getLong("favorites_count"),
			rs.getBoolean("favorited"),
			rs.getBoolean("following"));

	private static final String FROM = " from articles a join users u on u.id = a.author_id";

	private final JdbcClient jdbc;

	public ArticleQueryRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Newest first (created_at desc, id desc). Bodies are omitted (null) - list items have none. */
	public ArticlePage findPage(Filter filter, long viewerId, long offset, int limit) {
		Map<String, Object> params = params(filter, viewerId);
		String where = where(filter);
		long total = jdbc.sql("select count(*)" + FROM + where)
				.params(params)
				.query(Long.class)
				.single();
		if (total == 0 || offset >= total) {
			return new ArticlePage(List.of(), total);
		}
		return new ArticlePage(fetch(filter, viewerId, offset, limit, false), total);
	}

	public Optional<ArticleView> findBySlug(String slug, long viewerId) {
		return fetch(Filter.ofSlug(slug), viewerId, 0, 1, true).stream().findFirst();
	}

	private List<ArticleView> fetch(Filter filter, long viewerId, long offset, int limit, boolean withBody) {
		String select = """
				select a.id, a.slug, a.title, a.description, %s as body, a.created_at, a.updated_at,
				       u.username as author_username, u.bio as author_bio, u.image as author_image,
				       (select count(*) from favorites f where f.article_id = a.id) as favorites_count,
				       exists (select 1 from favorites f
				                where f.article_id = a.id and f.user_id = :viewerId) as favorited,
				       exists (select 1 from follows fo
				                where fo.follower_id = :viewerId and fo.followee_id = a.author_id) as following
				""".formatted(withBody ? "a.body" : "cast(null as text)");
		List<Row> rows = jdbc.sql(select + FROM + where(filter)
						+ " order by a.created_at desc, a.id desc limit :limit offset :offset")
				.params(params(filter, viewerId))
				.param("limit", limit)
				.param("offset", offset)
				.query(ROW_MAPPER)
				.list();
		if (rows.isEmpty()) {
			return List.of();
		}
		Map<Long, List<String>> tags = tagsFor(rows.stream().map(Row::id).toList());
		List<ArticleView> views = new ArrayList<>(rows.size());
		for (Row row : rows) {
			views.add(new ArticleView(
					row.slug(), row.title(), row.description(), row.body(),
					tags.getOrDefault(row.id(), List.of()),
					Timestamps.format(row.createdAt().toInstant()),
					Timestamps.format(row.updatedAt().toInstant()),
					row.favorited(), row.favoritesCount(),
					new ProfileView(row.authorUsername(), row.authorBio(), row.authorImage(), row.following())));
		}
		return views;
	}

	/** One query for the tags of every article on the page, in stored (first-seen) order. */
	private Map<Long, List<String>> tagsFor(List<Long> articleIds) {
		List<TagRow> rows = jdbc.sql("""
				select article_id, tag from article_tags
				 where article_id in (:ids)
				 order by article_id, tag_position""")
				.param("ids", articleIds)
				.query((rs, n) -> new TagRow(rs.getLong("article_id"), rs.getString("tag")))
				.list();
		Map<Long, List<String>> byArticle = new LinkedHashMap<>();
		for (TagRow row : rows) {
			byArticle.computeIfAbsent(row.articleId(), k -> new ArrayList<>()).add(row.tag());
		}
		return byArticle;
	}

	private static String where(Filter filter) {
		StringBuilder sql = new StringBuilder(" where 1 = 1");
		if (filter.slug() != null) {
			sql.append(" and a.slug = :slug");
		}
		if (filter.tag() != null) {
			sql.append(" and exists (select 1 from article_tags t where t.article_id = a.id and t.tag = :tag)");
		}
		if (filter.author() != null) {
			sql.append(" and lower(u.username) = lower(:author)");
		}
		if (filter.favoritedBy() != null) {
			sql.append(" and exists (select 1 from favorites ff join users fu on fu.id = ff.user_id"
					+ " where ff.article_id = a.id and lower(fu.username) = lower(:favoritedBy))");
		}
		if (filter.feedOnly()) {
			sql.append(" and exists (select 1 from follows ft"
					+ " where ft.follower_id = :viewerId and ft.followee_id = a.author_id)");
		}
		return sql.toString();
	}

	private static Map<String, Object> params(Filter filter, long viewerId) {
		Map<String, Object> params = new HashMap<>();
		params.put("viewerId", viewerId);
		if (filter.slug() != null) {
			params.put("slug", filter.slug());
		}
		if (filter.tag() != null) {
			params.put("tag", filter.tag());
		}
		if (filter.author() != null) {
			params.put("author", filter.author());
		}
		if (filter.favoritedBy() != null) {
			params.put("favoritedBy", filter.favoritedBy());
		}
		return params;
	}

	/** Slugs equal to {@code base} or of the form {@code base-N}, ignoring the article being edited. */
	public Set<String> findTakenSlugs(String base, long excludeArticleId) {
		// base only contains [a-z0-9-], so it holds no LIKE wildcards
		return new HashSet<>(jdbc.sql("""
				select slug from articles
				 where id <> :exclude and (slug = :base or slug like :pattern)""")
				.param("exclude", excludeArticleId)
				.param("base", base)
				.param("pattern", base + "-%")
				.query(String.class)
				.list());
	}

	/** The most used tags, by usage desc then alphabetically (byte order, so it is locale independent). */
	public List<String> popularTags(int limit) {
		return jdbc.sql("""
				select tag from article_tags
				 group by tag
				 order by count(*) desc, tag collate "C" asc
				 limit :limit""")
				.param("limit", limit)
				.query(String.class)
				.list();
	}

	public void addFavorite(long userId, long articleId) {
		jdbc.sql("insert into favorites (user_id, article_id) values (:user, :article) on conflict do nothing")
				.param("user", userId)
				.param("article", articleId)
				.update();
	}

	public void removeFavorite(long userId, long articleId) {
		jdbc.sql("delete from favorites where user_id = :user and article_id = :article")
				.param("user", userId)
				.param("article", articleId)
				.update();
	}
}
