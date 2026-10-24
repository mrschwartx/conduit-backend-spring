package com.conduit.backend.identity.infrastructure.persistence;

import com.conduit.backend.identity.domain.FollowRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class FollowRepositoryAdapter implements FollowRepository {

    private final FollowJpaRepository repository;

    FollowRepositoryAdapter(FollowJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(Long followerId, Long followeeId) {
        return repository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public void add(Long followerId, Long followeeId) {
        repository.save(new FollowEntity(followerId, followeeId));
    }

    @Override
    public void remove(Long followerId, Long followeeId) {
        repository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public Set<Long> findFolloweeIds(Long followerId) {
        return new HashSet<>(repository.findFolloweeIds(followerId));
    }
}
