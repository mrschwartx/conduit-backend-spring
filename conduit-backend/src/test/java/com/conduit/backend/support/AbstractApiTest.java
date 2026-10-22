package com.conduit.backend.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractApiTest {

	protected static final String V1 = "/api/v1";
	protected static final String PASSWORD = "password123";
	protected static final String TEST_JWT_SECRET = "integration-test-secret-0123456789-abcdef";

	private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:18.3")
			.withEnv("POSTGRES_DB", "conduit_test")
			.withEnv("POSTGRES_USER", "test")
			.withEnv("POSTGRES_PASSWORD", "test")
			.withExposedPorts(5432)
			.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":"
				+ POSTGRES.getMappedPort(5432) + "/conduit_test");
		registry.add("spring.datasource.username", () -> "test");
		registry.add("spring.datasource.password", () -> "test");
		registry.add("conduit.jwt-secret", () -> TEST_JWT_SECRET);
	}

	@Value("${local.server.port}")
	private int port;

	@Autowired
	protected JdbcClient jdbc;

	private final HttpClient client = HttpClient.newHttpClient();

	@BeforeEach
	void cleanDatabase() {
		jdbc.sql("truncate table users restart identity cascade").update();
	}


	public record Resp(int status, String body, HttpHeaders headers) {

		public <T> T json(String path) {
			return JsonPath.read(body, path);
		}

		public String header(String name) {
			return headers.firstValue(name).orElse(null);
		}
	}

	protected Resp get(String path, String token) {
		return send("GET", path, token, null, null);
	}

	protected Resp post(String path, String token, String json) {
		return send("POST", path, token, json, "application/json");
	}

	protected Resp put(String path, String token, String json) {
		return send("PUT", path, token, json, "application/json");
	}

	protected Resp delete(String path, String token) {
		return send("DELETE", path, token, null, null);
	}

	protected Resp send(String method, String path, String token, String body, String contentType) {
		return sendRaw(method, path, token == null ? null : "Token " + token, body, contentType);
	}

	protected Resp sendRaw(String method, String path, String authorization, String body, String contentType) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
		if (authorization != null) {
			builder.header("Authorization", authorization);
		}
		if (contentType != null) {
			builder.header("Content-Type", contentType);
		}
		builder.method(method, body == null ? BodyPublishers.noBody()
				: BodyPublishers.ofString(body, StandardCharsets.UTF_8));
		try {
			HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString());
			return new Resp(response.statusCode(), response.body(), response.headers());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}


	protected String register(String username) {
		Resp response = post(V1 + "/users", null, """
				{"user":{"username":"%s","email":"%s@example.com","password":"%s"}}"""
				.formatted(username, username, PASSWORD));
		if (response.status() != 201) {
			throw new IllegalStateException("register failed: " + response.status() + " " + response.body());
		}
		return response.json("$.user.token");
	}

	protected String createArticle(String token, String title, String... tags) {
		String tagJson = "[" + String.join(",", java.util.Arrays.stream(tags).map(t -> "\"" + t + "\"").toList()) + "]";
		Resp response = post(V1 + "/articles", token, """
				{"article":{"title":"%s","description":"d","body":"b","tagList":%s}}"""
				.formatted(title, tagJson));
		if (response.status() != 201) {
			throw new IllegalStateException("createArticle failed: " + response.status() + " " + response.body());
		}
		return response.json("$.article.slug");
	}
}
