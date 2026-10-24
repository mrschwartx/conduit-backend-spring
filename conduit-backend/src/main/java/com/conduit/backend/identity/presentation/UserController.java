package com.conduit.backend.identity.presentation;

import com.conduit.backend.identity.application.UserService;
import com.conduit.backend.identity.application.command.LoginCommand;
import com.conduit.backend.identity.application.command.RegisterUserCommand;
import com.conduit.backend.identity.application.command.UpdateUserCommand;
import com.conduit.backend.identity.presentation.dto.UserRequests.LoginRequest;
import com.conduit.backend.identity.presentation.dto.UserRequests.RegisterRequest;
import com.conduit.backend.identity.presentation.dto.UserRequests.UpdateRequest;
import com.conduit.backend.identity.presentation.dto.UserResponses.UserResponse;
import com.conduit.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "User and Authentication")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users/login")
    @Operation(operationId = "Login", summary = "Existing user login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        return UserResponse.from(userService.login(
                new LoginCommand(request.user().email(), request.user().password())));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "CreateUser", summary = "Register a new user")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(new RegisterUserCommand(
                request.user().username(), request.user().email(), request.user().password())));
    }

    @GetMapping("/user")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "GetCurrentUser", summary = "Get current user")
    public UserResponse current(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(userService.getCurrent(principal.userId()));
    }

    @PutMapping("/user")
    @SecurityRequirement(name = "Token")
    @Operation(operationId = "UpdateCurrentUser", summary = "Update current user")
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                               @Valid @RequestBody UpdateRequest request) {
        return UserResponse.from(userService.update(principal.userId(), new UpdateUserCommand(
                request.user().username(), request.user().email(), request.user().password(),
                request.user().bio(), request.user().image())));
    }
}
