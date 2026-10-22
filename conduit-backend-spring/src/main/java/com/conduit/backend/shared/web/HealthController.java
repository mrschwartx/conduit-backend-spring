package com.conduit.backend.shared.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("OK");
	}
}
