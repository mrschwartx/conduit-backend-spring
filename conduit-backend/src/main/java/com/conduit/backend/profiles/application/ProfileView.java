package com.conduit.backend.profiles.application;

public record ProfileView(String username, String bio, String image, boolean following) {
}
