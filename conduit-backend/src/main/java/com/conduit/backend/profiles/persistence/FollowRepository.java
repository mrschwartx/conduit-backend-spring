package com.conduit.backend.profiles.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FollowRepository {

	private final JdbcClient jdbc;

	public FollowRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public boolean exists(long followerId, long followeeId) {
		return jdbc.sql("select exists (select 1 from follows where follower_id = :follower and followee_id = :followee)")
				.param("follower", followerId)
				.param("followee", followeeId)
				.query(Boolean.class)
				.single();
	}

	public void add(long followerId, long followeeId) {
		jdbc.sql("insert into follows (follower_id, followee_id) values (:follower, :followee) on conflict do nothing")
				.param("follower", followerId)
				.param("followee", followeeId)
				.update();
	}

	public void remove(long followerId, long followeeId) {
		jdbc.sql("delete from follows where follower_id = :follower and followee_id = :followee")
				.param("follower", followerId)
				.param("followee", followeeId)
				.update();
	}
}
