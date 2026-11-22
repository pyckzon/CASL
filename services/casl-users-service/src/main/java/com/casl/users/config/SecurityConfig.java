package com.casl.users.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resource server JWT: deny-by-default (todo requiere token salvo health),
 * roles de Keycloak ({@code realm_access.roles}) y errores 401 en
 * {@code application/problem+json} para respetar el contrato RFC 9457.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final ObjectMapper objectMapper;

	public SecurityConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(this::unauthorized))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(this::unauthorized)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
		return converter;
	}

	private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"A valid access token is required");
		problem.setType(URI.create("https://errors.casl.dev/unauthorized"));
		problem.setTitle("Authentication required");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", "UNAUTHORIZED");
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

	/**
	 * Keycloak publica los roles de realm en {@code realm_access.roles}; Spring
	 * Security los consume como {@code ROLE_*}.
	 */
	static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
		@Override
		public Collection<GrantedAuthority> convert(Jwt jwt) {
			Map<String, Object> realmAccess = jwt.getClaim("realm_access");
			if (realmAccess == null) {
				return List.of();
			}
			Object roles = realmAccess.get("roles");
			if (!(roles instanceof List)) {
				return List.of();
			}
			return ((List<?>) roles).stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
					.collect(Collectors.toList());
		}
	}
}
