package com.casl.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

	private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

	@Test
	void mapsRealmRolesToSpringAuthorities() {
		Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("USER", "ADMIN"))));

		Collection<GrantedAuthority> authorities = converter.convert(jwt);

		assertThat(authorities).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
	}

	@Test
	void tokenWithoutRolesHasNoAuthorities() {
		assertThat(converter.convert(jwt(Map.of()))).isEmpty();
		assertThat(converter.convert(jwt(Map.of("realm_access", Map.of("roles", "ADMIN"))))).isEmpty();
	}

	@Test
	void ignoresNonStringRoles() {
		Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("USER", 42, " "))));

		assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_USER");
	}

	private static Jwt jwt(Map<String, Object> claims) {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("11111111-1111-1111-1111-111111111111")
				.claims(map -> map.putAll(claims))
				.build();
	}
}
