package com.conduit.backend.identity.application.view;

public record ProfileView(String username, String bio, String image, boolean following) {
}
