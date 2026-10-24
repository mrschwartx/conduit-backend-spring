package com.conduit.backend.identity.application.command;

public record RegisterUserCommand(String username, String email, String password) {
}
