package com.casl.order.adapter.in.web;

import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.config.SecurityConfig;
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
 * Estado del usuario en pedidos: un usuario inactivo no opera aunque el JWT
 * siga válido (ADR-018).
 */
@WebMvcTest(value = OrderController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserStatusSecurityTest {

	private static final String BASE = "/api/orders/v1/orders";
	private static final UUID ORDER_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000005");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderUseCase useCase;

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
		mockMvc.perform(get(BASE + "/{orderId}", ORDER_ID).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}

	@Test
	void inactiveUserWithAdminRoleGetsUserStatusForbidden() throws Exception {
		mockMvc.perform(get(BASE + "/{orderId}", ORDER_ID).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		verifyNoInteractions(useCase);
	}
}
