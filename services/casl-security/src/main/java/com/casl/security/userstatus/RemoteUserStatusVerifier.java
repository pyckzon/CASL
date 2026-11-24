package com.casl.security.userstatus;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Cliente remoto: reenvía el JWT del llamador al endpoint de estado del
 * servicio de usuarios. Sin secreto adicional: el token del llamador es la
 * credencial usada.
 */
public class RemoteUserStatusVerifier implements UserStatusVerifier {

	private static final String STATUS_URI = "/api/users/v1/users/me/status";

	private final RestClient restClient;

	public RemoteUserStatusVerifier(CaslUserStatusProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeout());
		factory.setReadTimeout(properties.getReadTimeout());
		this.restClient = RestClient.builder().requestFactory(factory).baseUrl(properties.getBaseUrl()).build();
	}

	@Override
	public UserStatus verify(Jwt jwt) {
		try {
			UserStatusResponse response = restClient.get()
					.uri(STATUS_URI)
					.header("Authorization", "Bearer " + jwt.getTokenValue())
					.retrieve()
					.body(UserStatusResponse.class);
			if (response == null || response.status() == null) {
				throw new UserStatusUnavailableException("Empty response from users service");
			}
			return response.status();
		}
		catch (UserStatusUnavailableException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new UserStatusUnavailableException("Unable to verify user status", exception);
		}
	}
}
