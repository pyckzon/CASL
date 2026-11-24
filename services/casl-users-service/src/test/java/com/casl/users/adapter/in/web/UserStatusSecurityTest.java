package com.casl.users.adapter.in.web;

import com.casl.security.userstatus.UserStatus;
import com.casl.security.userstatus.UserStatusVerifier;
import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.port.in.UserUseCase;
import com.casl.users.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Estado del usuario en el servicio dueño: un usuario inactivo (USER o ADMIN)
 * no opera en endpoints protegidos (ADR-018).
 */
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserStatusSecurityTest {

	private static final String BASE = "/api/users/v1/users";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@TestConfiguration
	static class StatusSecurityConfig {

		@Bean
		UserStatusVerifier userStatusVerifier() {
			return jwt -> UserStatus.INACTIVE;
		}
	}

	@Test
	void inactiveUserWithUserRoleGetsUserStatusForbidden() throws Exception {
		mockMvc.perform(get(BASE).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}

	@Test
	void inactiveUserWithAdminRoleGetsUserStatusForbidden() throws Exception {
		mockMvc.perform(get(BASE).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}

	@Test
	void meStatusRemainsAccessibleForInactiveUser() throws Exception {
		when(useCase.statusOf(any(IdentityProfile.class)))
				.thenReturn(com.casl.users.domain.model.UserStatus.INACTIVE);

		mockMvc.perform(get(BASE + "/me/status").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
	}
}
