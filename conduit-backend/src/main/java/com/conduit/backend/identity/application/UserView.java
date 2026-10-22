package com.conduit.backend.identity.application;

public record UserView(String email, String token, String username, String bio, String image) {
}
