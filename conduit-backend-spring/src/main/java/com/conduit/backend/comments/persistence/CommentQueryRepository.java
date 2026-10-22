package com.conduit.backend.comments.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.conduit.backend.comments.application.CommentView;
import com.conduit.backend.profiles.application.ProfileView;
import com.conduit.backend.shared.web.Timestamps;

/**
 * Read side for comments. A single query per call assembles the comment, its author
 * profile and the viewer's "following" flag (no N+1 when listing).
 */
@Repository
public class CommentQueryRepository {

	private static final String SELECT = """
			select c.id, c.body, c.created_at, c.updated_at, c.article_id, c.author_id,
			       u.username as author_username, u.bio as author_bio, u.image as author_image,
			       exists (select 1 from follows fo
			                where fo.follower_id = :viewerId and fo.followee_id = c.author_id) as following
			  from comments c join users u on u.id = c.author_id
			""";

	/** A stored comment together with the ids needed for existence/ownership checks. */
	public record Stored(long articleId, long authorId, CommentView view) {
	}

	private static final RowMapper<Stored> MAPPER = (rs, n) -> new Stored(
			rs.getLong("article_id"),
			rs.getLong("author_id"),
			new CommentView(
					rs.getLong("id"),
					Timestamps.format(rs.getObject("created_at", OffsetDateTime.class).toInstant()),
					Timestamps.format(rs.getObject("updated_at", OffsetDateTime.class).toInstant()),
					rs.getString("body"),
					new ProfileView(rs.getString("author_username"), rs.getString("author_bio"),
							rs.getString("author_image"), rs.getBoolean("following"))));

	private final JdbcClient jdbc;

	public CommentQueryRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Long> findArticleId(String slug) {
		return jdbc.sql("select id from articles where slug = :slug")
				.param("slug", slug)
				.query(Long.class)
				.optional();
	}

	/** Newest first (created_at desc, id desc). */
	public List<CommentView> findByArticle(long articleId, long viewerId) {
		return jdbc.sql(SELECT + " where c.article_id = :articleId order by c.created_at desc, c.id desc")
				.param("articleId", articleId)
				.param("viewerId", viewerId)
				.query(MAPPER)
				.list()
				.stream()
				.map(Stored::view)
				.toList();
	}

	public Optional<Stored> findById(long commentId, long viewerId) {
		return jdbc.sql(SELECT + " where c.id = :id")
				.param("id", commentId)
				.param("viewerId", viewerId)
				.query(MAPPER)
				.optional();
	}
}
