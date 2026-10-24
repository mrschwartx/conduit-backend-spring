package com.conduit.backend.identity.application;

import com.conduit.backend.identity.contract.UserDirectory;
import com.conduit.backend.identity.contract.UserSummary;
import com.conduit.backend.identity.domain.FollowRepository;
import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.domain.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectory {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public UserDirectoryService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Override
    public Optional<UserSummary> findById(Long id) {
        return userRepository.findById(id).map(this::toSummary);
    }

    @Override
    public Optional<UserSummary> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toSummary);
    }

    @Override
    public Map<Long, UserSummary> findAllByIds(Collection<Long> ids) {
        Map<Long, UserSummary> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        for (User user : userRepository.findAllByIds(ids)) {
            result.put(user.getId(), toSummary(user));
        }
        return result;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.exists(followerId, followeeId);
    }

    @Override
    public Set<Long> findFollowedIds(Long followerId) {
        return followRepository.findFolloweeIds(followerId);
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getBio(), user.getImage());
    }
}
