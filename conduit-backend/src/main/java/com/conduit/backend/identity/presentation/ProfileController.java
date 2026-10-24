package com.conduit.backend.identity.presentation;

import com.conduit.backend.identity.application.ProfileService;
import com.conduit.backend.identity.presentation.dto.UserResponses.ProfileResponse;
import com.conduit.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{username}")
    @Operation(operationId = "GetProfileByUsername", summary = "Get a profile")
    public ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String username) {
        Long viewerId = principal == null ? null : principal.userId();
        return ProfileResponse.from(profileService.getProfile(viewerId, username));
    }

    @PostMapping("/{username}/follow")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "FollowUserByUsername", summary = "Follow a user")
    public ProfileResponse follow(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String username) {
        return ProfileResponse.from(profileService.follow(principal.userId(), username));
    }

    @DeleteMapping("/{username}/follow")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "UnfollowUserByUsername", summary = "Unfollow a user")
    public ProfileResponse unfollow(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String username) {
        return ProfileResponse.from(profileService.unfollow(principal.userId(), username));
    }
}
