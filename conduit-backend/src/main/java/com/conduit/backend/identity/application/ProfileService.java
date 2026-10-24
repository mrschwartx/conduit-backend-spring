package com.conduit.backend.identity.application;

import com.conduit.backend.identity.application.view.ProfileView;
import com.conduit.backend.identity.domain.FollowRepository;
import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.domain.UserRepository;
import com.conduit.backend.shared.kernel.InvalidInputException;
import com.conduit.backend.shared.kernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public ProfileService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public ProfileView getProfile(Long viewerId, String username) {
        User user = findByUsername(username);
        boolean following = viewerId != null && followRepository.exists(viewerId, user.getId());
        return toView(user, following);
    }

    public ProfileView follow(Long followerId, String username) {
        User user = findByUsername(username);
        if (user.getId().equals(followerId)) {
            throw new InvalidInputException("username", "cannot follow yourself");
        }
        if (!followRepository.exists(followerId, user.getId())) {
            followRepository.add(followerId, user.getId());
        }
        return toView(user, true);
    }

    public ProfileView unfollow(Long followerId, String username) {
        User user = findByUsername(username);
        if (followRepository.exists(followerId, user.getId())) {
            followRepository.remove(followerId, user.getId());
        }
        return toView(user, false);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("profile"));
    }

    private ProfileView toView(User user, boolean following) {
        return new ProfileView(user.getUsername(), user.getBio(), user.getImage(), following);
    }
}
