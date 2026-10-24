package com.conduit.backend.identity.application;

import com.conduit.backend.identity.application.command.LoginCommand;
import com.conduit.backend.identity.application.command.RegisterUserCommand;
import com.conduit.backend.identity.application.command.UpdateUserCommand;
import com.conduit.backend.identity.application.port.PasswordHasher;
import com.conduit.backend.identity.application.port.TokenIssuer;
import com.conduit.backend.identity.application.view.UserView;
import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.domain.UserRepository;
import com.conduit.backend.shared.kernel.ConflictException;
import com.conduit.backend.shared.kernel.InvalidInputException;
import com.conduit.backend.shared.kernel.NotFoundException;
import com.conduit.backend.shared.kernel.UnauthorizedException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private static final String TAKEN = "has already been taken";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    public UserView register(RegisterUserCommand command) {
        String username = command.username().trim();
        String email = normalizeEmail(command.email());
        Map<String, List<String>> conflicts = new LinkedHashMap<>();
        if (userRepository.existsByUsername(username)) {
            conflicts.put("username", List.of(TAKEN));
        }
        if (userRepository.existsByEmail(email)) {
            conflicts.put("email", List.of(TAKEN));
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts);
        }
        User user = User.register(username, email, passwordHasher.hash(command.password()));
        return toView(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserView login(LoginCommand command) {
        User user = userRepository.findByEmail(normalizeEmail(command.email()))
                .filter(candidate -> passwordHasher.matches(command.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("email or password", "is invalid"));
        return toView(user);
    }

    @Transactional(readOnly = true)
    public UserView getCurrent(Long userId) {
        return toView(findUser(userId));
    }

    public UserView update(Long userId, UpdateUserCommand command) {
        if (command.isEmpty()) {
            throw new InvalidInputException("user", "at least one field is required");
        }
        User user = findUser(userId);
        String username = command.username() == null ? null : command.username().trim();
        String email = command.email() == null ? null : normalizeEmail(command.email());
        Map<String, List<String>> conflicts = new LinkedHashMap<>();
        if (username != null && !username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
            conflicts.put("username", List.of(TAKEN));
        }
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            conflicts.put("email", List.of(TAKEN));
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts);
        }
        if (username != null) {
            user.rename(username);
        }
        if (email != null) {
            user.changeEmail(email);
        }
        if (command.password() != null) {
            user.changePasswordHash(passwordHasher.hash(command.password()));
        }
        if (command.bio() != null) {
            user.changeBio(command.bio());
        }
        if (command.image() != null) {
            user.changeImage(command.image());
        }
        return toView(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user"));
    }

    private UserView toView(User user) {
        return new UserView(user.getEmail(), tokenIssuer.issue(user.getId()), user.getUsername(),
                user.getBio(), user.getImage());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
