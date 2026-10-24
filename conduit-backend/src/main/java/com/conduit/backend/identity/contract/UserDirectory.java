package com.conduit.backend.identity.contract;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserDirectory {

    Optional<UserSummary> findById(Long id);

    Optional<UserSummary> findByUsername(String username);

    Map<Long, UserSummary> findAllByIds(Collection<Long> ids);

    boolean isFollowing(Long followerId, Long followeeId);

    Set<Long> findFollowedIds(Long followerId);
}
