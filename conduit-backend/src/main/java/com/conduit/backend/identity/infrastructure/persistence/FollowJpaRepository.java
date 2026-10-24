package com.conduit.backend.identity.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface FollowJpaRepository extends JpaRepository<FollowEntity, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    @Query("select f.followeeId from FollowEntity f where f.followerId = :followerId")
    List<Long> findFolloweeIds(Long followerId);
}
