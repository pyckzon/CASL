package com.casl.security.userstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RemoteUserStatusVerifierTest {

	private static final String STATUS_PATH = "/api/users/v1/users/me/status";

	private HttpServer server;
	private int port;
	private volatile boolean failWithServerError;
	private final AtomicReference<String> authorization = new AtomicReference<>();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		port = server.getAddress().getPort();
		server.createContext(STATUS_PATH, exchange -> {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			if (failWithServerError) {
				exchange.sendResponseHeaders(500, 0);
				exchange.close();
				return;
			}
			byte[] body = "{\"status\":\"INACTIVE\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(body);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void returnsInactiveStatusAndForwardsBearerToken() {
		RemoteUserStatusVerifier verifier = new RemoteUserStatusVerifier(properties());

		UserStatus status = verifier.verify(
				Jwt.withTokenValue("token-value").header("alg", "none").claim("sub", "sub-1").build());

		assertThat(status).isEqualTo(UserStatus.INACTIVE);
		assertThat(authorization.get()).isEqualTo("Bearer token-value");
	}

	@Test
	void serverErrorYieldsUserStatusUnavailable() {
		failWithServerError = true;
		RemoteUserStatusVerifier verifier = new RemoteUserStatusVerifier(properties());

		assertThatThrownBy(() -> verifier.verify(
				Jwt.withTokenValue("token-value").header("alg", "none").claim("sub", "sub-1").build()))
				.isInstanceOf(UserStatusUnavailableException.class);
	}

	private CaslUserStatusProperties properties() {
		CaslUserStatusProperties properties = new CaslUserStatusProperties();
		properties.setBaseUrl("http://localhost:" + port);
		return properties;
	}
}
