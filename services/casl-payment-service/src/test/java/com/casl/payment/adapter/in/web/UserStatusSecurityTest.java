package com.casl.payment.adapter.in.web;

import com.casl.payment.application.port.in.PaymentUseCase;
import com.casl.payment.config.SecurityConfig;
import com.casl.security.userstatus.UserStatus;
import com.casl.security.userstatus.UserStatusVerifier;
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

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Estado del usuario en pagos: un usuario inactivo no opera aunque el JWT
 * siga válido (ADR-018).
 */
@WebMvcTest(value = PaymentController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserStatusSecurityTest {

	private static final String BASE = "/api/payments/v1/payments";
	private static final UUID PAYMENT_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000007");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentUseCase useCase;

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
		mockMvc.perform(get(BASE + "/" + PAYMENT_ID).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}

	@Test
	void inactiveUserWithAdminRoleGetsUserStatusForbidden() throws Exception {
		mockMvc.perform(get(BASE + "/" + PAYMENT_ID).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}
}
