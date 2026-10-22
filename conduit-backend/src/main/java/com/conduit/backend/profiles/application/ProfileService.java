package com.conduit.backend.profiles.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.persistence.UserRepository;
import com.conduit.backend.profiles.persistence.FollowRepository;
import com.conduit.backend.shared.errors.ApiException;
import com.conduit.backend.shared.security.AuthUser;

@Service
public class ProfileService {

	private final UserRepository users;
	private final FollowRepository follows;

	public ProfileService(UserRepository users, FollowRepository follows) {
		this.users = users;
		this.follows = follows;
	}

	@Transactional(readOnly = true)
	public ProfileView get(String username, AuthUser viewer) {
		User target = find(username);
		boolean following = viewer != null && follows.exists(viewer.id(), target.getId());
		return view(target, following);
	}

	@Transactional
	public ProfileView follow(AuthUser viewer, String username) {
		User target = find(username);
		if (target.getId() == viewer.id()) {
			throw ApiException.of(ApiException.UNPROCESSABLE, "username", "you cannot follow yourself");
		}
		follows.add(viewer.id(), target.getId());
		return view(target, true);
	}

	@Transactional
	public ProfileView unfollow(AuthUser viewer, String username) {
		User target = find(username);
		follows.remove(viewer.id(), target.getId());
		return view(target, false);
	}

	private User find(String username) {
		return users.findByUsername(username).orElseThrow(() -> ApiException.notFound("profile"));
	}

	private static ProfileView view(User user, boolean following) {
		return new ProfileView(user.getUsername(), user.getBio(), user.getImage(), following);
	}
}
