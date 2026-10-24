package com.conduit.backend.identity.application.view;

public record UserView(String email, String token, String username, String bio, String image) {
}
