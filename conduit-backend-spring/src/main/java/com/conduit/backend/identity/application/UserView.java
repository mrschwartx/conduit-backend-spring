package com.conduit.backend.identity.application;

/** Contract "User" object. bio/image are always serialised (null when unset). */
public record UserView(String email, String token, String username, String bio, String image) {
}
