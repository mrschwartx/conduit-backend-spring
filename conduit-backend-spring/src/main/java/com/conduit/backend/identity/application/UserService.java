package com.conduit.backend.identity.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.persistence.UserRepository;
import com.conduit.backend.shared.errors.ApiException;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.security.JwtService;
import com.conduit.backend.shared.validation.FieldErrors;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.validation.Text;

@Service
public class UserService {

	private static final String TAKEN = "has already been taken";

	private final UserRepository users;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final Clock clock;
	/** Compared against when the email is unknown, so both failures cost the same time. */
	private final String dummyHash;

	public UserService(UserRepository users, PasswordEncoder encoder, JwtService jwt, Clock clock) {
		this.users = users;
		this.encoder = encoder;
		this.jwt = jwt;
		this.clock = clock;
		this.dummyHash = encoder.encode("not-a-real-password");
	}

	@Transactional
	public UserView register(Fields fields) {
		FieldErrors errors = new FieldErrors();
		String username = UserRules.username(fields.get("username"), errors);
		String email = UserRules.email(fields.get("email"), errors);
		String password = UserRules.password(fields.get("password"), errors);
		errors.throwUnprocessableIfAny();

		FieldErrors conflicts = new FieldErrors();
		if (users.countByUsername(username, 0L) > 0) {
			conflicts.add("username", TAKEN);
		}
		if (users.countByEmail(email, 0L) > 0) {
			conflicts.add("email", TAKEN);
		}
		conflicts.throwConflictIfAny();

		User user = users.saveAndFlush(new User(username, email, encoder.encode(password), now()));
		return view(user);
	}

	@Transactional(readOnly = true)
	public UserView login(Fields fields) {
		FieldErrors errors = new FieldErrors();
		String email = Text.required(fields.get("email"), "email", errors);
		String password = UserRules.rawPassword(fields.get("password"), "password", errors);
		errors.throwUnprocessableIfAny();

		User user = users.findByEmail(email).orElse(null);
		// bcrypt only accepts 72 bytes; a longer password can never have been stored
		boolean candidateUsable = password.getBytes(StandardCharsets.UTF_8).length <= UserRules.PASSWORD_MAX;
		String hash = user != null ? user.getPasswordHash() : dummyHash;
		boolean matches = encoder.matches(candidateUsable ? password : "x", hash);
		if (user == null || !candidateUsable || !matches) {
			// same response for unknown email and wrong password
			throw ApiException.unauthorized("credentials", "are invalid");
		}
		return view(user);
	}

	@Transactional(readOnly = true)
	public UserView current(AuthUser auth) {
		return view(load(auth));
	}

	@Transactional
	public UserView update(AuthUser auth, Fields fields) {
		User user = load(auth);
		if (!(fields.has("email") || fields.has("username") || fields.has("password")
				|| fields.has("bio") || fields.has("image"))) {
			throw ApiException.of(ApiException.UNPROCESSABLE, "user", "at least one field is required");
		}

		FieldErrors errors = new FieldErrors();
		String username = fields.has("username") ? UserRules.username(fields.get("username"), errors) : null;
		String email = fields.has("email") ? UserRules.email(fields.get("email"), errors) : null;
		String password = fields.has("password") ? UserRules.password(fields.get("password"), errors) : null;
		String bio = fields.has("bio") ? UserRules.bio(fields.get("bio"), errors) : null;
		String image = fields.has("image") ? UserRules.image(fields.get("image"), errors) : null;
		errors.throwUnprocessableIfAny();

		// uniqueness ignores the user's own record
		FieldErrors conflicts = new FieldErrors();
		if (username != null && users.countByUsername(username, user.getId()) > 0) {
			conflicts.add("username", TAKEN);
		}
		if (email != null && users.countByEmail(email, user.getId()) > 0) {
			conflicts.add("email", TAKEN);
		}
		conflicts.throwConflictIfAny();

		if (username != null) {
			user.setUsername(username);
		}
		if (email != null) {
			user.setEmail(email);
		}
		if (password != null) {
			user.setPasswordHash(encoder.encode(password));
		}
		if (fields.has("bio")) {
			user.setBio(bio);
		}
		if (fields.has("image")) {
			user.setImage(image);
		}
		user.setUpdatedAt(now());
		users.saveAndFlush(user);
		return view(user);
	}

	private User load(AuthUser auth) {
		return users.findById(auth.id()).orElseThrow(() -> ApiException.unauthorized("token", "is invalid"));
	}

	private UserView view(User user) {
		return new UserView(user.getEmail(), jwt.issue(user.getId(), user.getUsername()), user.getUsername(),
				user.getBio(), user.getImage());
	}

	private Instant now() {
		return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
	}
}
