package com.conduit.backend.identity.presentation.dto;

import com.conduit.backend.identity.application.view.ProfileView;
import com.conduit.backend.identity.application.view.UserView;

public final class UserResponses {

    private UserResponses() {
    }

    public record UserResponse(UserBody user) {

        public static UserResponse from(UserView view) {
            return new UserResponse(new UserBody(view.email(), view.token(), view.username(), view.bio(), view.image()));
        }
    }

    public record UserBody(String email, String token, String username, String bio, String image) {
    }

    public record ProfileResponse(ProfileBody profile) {

        public static ProfileResponse from(ProfileView view) {
            return new ProfileResponse(
                    new ProfileBody(view.username(), view.bio(), view.image(), view.following()));
        }
    }

    public record ProfileBody(String username, String bio, String image, boolean following) {
    }
}
