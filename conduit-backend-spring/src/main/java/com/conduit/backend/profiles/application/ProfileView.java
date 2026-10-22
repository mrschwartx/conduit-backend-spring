package com.conduit.backend.profiles.application;

/** Contract "Profile" object. bio/image are always serialised (null when unset). */
public record ProfileView(String username, String bio, String image, boolean following) {
}
