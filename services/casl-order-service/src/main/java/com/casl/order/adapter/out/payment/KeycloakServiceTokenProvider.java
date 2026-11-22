package com.casl.order.adapter.out.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Obtiene un token de cuenta de servicio (client credentials) para llamar a
 * payment. El token se cachea hasta 30 segundos antes de expirar: evita un
 * viaje a Keycloak en cada cobro sin introducir un cliente OAuth2 completo.
 */
@Component
public class KeycloakServiceTokenProvider {

	private static final int EXPIRY_MARGIN_SECONDS = 30;

	private final RestClient restClient;
	private final String tokenUri;
	private final String clientId;
	private final String clientSecret;

	private volatile CachedToken cached;

	public KeycloakServiceTokenProvider(
			@Value("${KEYCLOAK_TOKEN_URI:http://localhost:8180/realms/casl/protocol/openid-connect/token}") String tokenUri,
			@Value("${KEYCLOAK_CLIENT_ID:casl-order-service}") String clientId,
			@Value("${KEYCLOAK_CLIENT_SECRET:casl-order-secret}") String clientSecret) {
		this.restClient = RestClient.create();
		this.tokenUri = tokenUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public synchronized String get() {
		Instant now = Instant.now();
		if (cached != null && now.plusSeconds(EXPIRY_MARGIN_SECONDS).isBefore(cached.expiresAt())) {
			return cached.value();
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		Map<String, Object> response = restClient.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		if (response == null || !(response.get("access_token") instanceof String token)) {
			throw new IllegalStateException("Keycloak did not return an access token");
		}
		long expiresIn = response.get("expires_in") instanceof Number number ? number.longValue() : 60L;
		cached = new CachedToken(token, now.plusSeconds(expiresIn));
		return token;
	}

	private record CachedToken(String value, Instant expiresAt) {
	}
}
