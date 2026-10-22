package com.conduit.backend.identity.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conduit.backend.identity.application.UserService;
import com.conduit.backend.shared.security.AuthUser;
import com.conduit.backend.shared.validation.Fields;
import com.conduit.backend.shared.web.Api;

@RestController
@RequestMapping(Api.V1)
public class UserController {

	private final UserService users;

	public UserController(UserService users) {
		this.users = users;
	}

	@PostMapping("/users")
	public ResponseEntity<UserEnvelope> register(@RequestBody Map<String, Object> body) {
		return ResponseEntity.status(201).body(new UserEnvelope(users.register(Fields.envelope(body, "user"))));
	}

	@PostMapping("/users/login")
	public UserEnvelope login(@RequestBody Map<String, Object> body) {
		return new UserEnvelope(users.login(Fields.envelope(body, "user")));
	}

	@GetMapping("/user")
	public UserEnvelope current(@AuthenticationPrincipal AuthUser auth) {
		return new UserEnvelope(users.current(auth));
	}

	@PutMapping("/user")
	public UserEnvelope update(@AuthenticationPrincipal AuthUser auth, @RequestBody Map<String, Object> body) {
		return new UserEnvelope(users.update(auth, Fields.envelope(body, "user")));
	}
}
