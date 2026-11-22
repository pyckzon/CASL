package com.casl.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuración de seguridad compartida por los seis servicios CASL. Se importa
 * explícitamente desde cada servicio (`@Import`), sin auto-configuración
 * implícita: el cableado queda visible en el código de cada aplicación.
 *
 * <p>El patrón deny-by-default es uniforme: health público, {@code /api/**}
 * con JWT válido y cualquier otra ruta rechazada.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class CaslSecurityConfiguration {

	@Bean
	@ConditionalOnMissingBean(AuthenticationEntryPoint.class)
	public AuthenticationEntryPoint caslAuthenticationEntryPoint(ObjectMapper objectMapper) {
		return new ProblemDetailAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean(AccessDeniedHandler.class)
	public AccessDeniedHandler caslAccessDeniedHandler(ObjectMapper objectMapper) {
		return new ProblemDetailAccessDeniedHandler(objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean(JwtAuthenticationConverter.class)
	public JwtAuthenticationConverter caslJwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean(SecurityFilterChain.class)
	public SecurityFilterChain caslSecurityFilterChain(HttpSecurity http,
			AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		// API stateless: el JWT viaja en Authorization: Bearer, nunca en cookies. No hay
		// credenciales ambientales que un navegador adjunte de forma automática, por lo que
		// ninguna petición necesita protección CSRF.
		http
				.csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}
}
