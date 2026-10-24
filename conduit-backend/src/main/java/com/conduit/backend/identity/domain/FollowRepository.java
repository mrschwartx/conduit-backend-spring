package com.conduit.backend.identity.domain;

import java.util.Set;

public interface FollowRepository {

    boolean exists(Long followerId, Long followeeId);

    void add(Long followerId, Long followeeId);

    void remove(Long followerId, Long followeeId);

    Set<Long> findFolloweeIds(Long followerId);
}
