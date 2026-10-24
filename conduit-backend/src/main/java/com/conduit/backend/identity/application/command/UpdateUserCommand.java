package com.conduit.backend.identity.application.command;

public record UpdateUserCommand(String username, String email, String password, String bio, String image) {

    public boolean isEmpty() {
        return username == null && email == null && password == null && bio == null && image == null;
    }
}
