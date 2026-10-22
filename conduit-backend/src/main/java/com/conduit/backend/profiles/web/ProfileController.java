package com.conduit.backend.profiles.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conduit.backend.profiles.application.ProfileService;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.web.Api;

@RestController
@RequestMapping(Api.V1 + "/profiles/{username}")
public class ProfileController {

	private final ProfileService profiles;

	public ProfileController(ProfileService profiles) {
		this.profiles = profiles;
	}

	@GetMapping
	public ProfileEnvelope get(@PathVariable String username, @AuthenticationPrincipal AuthUser viewer) {
		return new ProfileEnvelope(profiles.get(username, viewer));
	}

	@PostMapping("/follow")
	public ProfileEnvelope follow(@PathVariable String username, @AuthenticationPrincipal AuthUser viewer) {
		return new ProfileEnvelope(profiles.follow(viewer, username));
	}

	@DeleteMapping("/follow")
	public ProfileEnvelope unfollow(@PathVariable String username, @AuthenticationPrincipal AuthUser viewer) {
		return new ProfileEnvelope(profiles.unfollow(viewer, username));
	}
}
