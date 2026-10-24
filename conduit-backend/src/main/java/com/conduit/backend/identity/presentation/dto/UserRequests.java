package com.conduit.backend.identity.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserRequests {

    private UserRequests() {
    }

    public record LoginRequest(@NotNull(message = "can't be blank") @Valid LoginBody user) {
    }

    public record LoginBody(
            @NotBlank(message = "can't be blank") String email,
            @NotBlank(message = "can't be blank") String password) {
    }

    public record RegisterRequest(@NotNull(message = "can't be blank") @Valid RegisterBody user) {
    }

    public record RegisterBody(
            @NotBlank(message = "can't be blank") @Size(max = 100, message = "is too long") String username,
            @NotBlank(message = "can't be blank") @Email(message = "is invalid")
            @Size(max = 255, message = "is too long") String email,
            @NotBlank(message = "can't be blank")
            @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password) {
    }

    public record UpdateRequest(@NotNull(message = "can't be blank") @Valid UpdateBody user) {
    }

    public record UpdateBody(
            @Pattern(regexp = ".*\\S.*", message = "can't be blank") @Size(max = 100, message = "is too long") String username,
            @Pattern(regexp = ".*\\S.*", message = "can't be blank") @Email(message = "is invalid")
            @Size(max = 255, message = "is too long") String email,
            @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
            @Size(max = 2000, message = "is too long") String bio,
            @Size(max = 1000, message = "is too long") String image) {
    }
}
