package com.conduit.backend.identity.application;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.conduit.backend.identity.persistence.UserRepository;
import com.conduit.backend.shared.security.AuthUserLookup;

@Component
public class UserAuthLookup implements AuthUserLookup {

	private final UserRepository users;

	public UserAuthLookup(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> findUsernameById(long id) {
		return users.findUsernameById(id);
	}
}
