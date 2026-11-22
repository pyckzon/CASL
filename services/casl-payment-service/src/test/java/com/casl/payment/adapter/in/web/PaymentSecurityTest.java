package com.casl.payment.adapter.in.web;

import com.casl.payment.application.port.in.PaymentUseCase;
import com.casl.payment.config.SecurityConfig;
import com.casl.payment.domain.model.Payment;
import com.casl.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Solo ADMIN o una cuenta de servicio pueden cobrar/devolver. */
@WebMvcTest(value = PaymentController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class PaymentSecurityTest {

	private static final String BASE = "/api/payments/v1/payments";
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final String BODY = """
			{"amount":19.98,"currency":"USD","idempotencyKey":"key-1"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void withoutTokenReturns401() throws Exception {
		mockMvc.perform(post(BASE).header("Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void userRoleCannotCharge() throws Exception {
		mockMvc.perform(post(BASE).with(token(UUID.randomUUID().toString(), "ROLE_USER"))
				.header("Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void userRoleCannotReadPayment() throws Exception {
		mockMvc.perform(get(BASE + "/" + UUID.randomUUID())
						.with(token(UUID.randomUUID().toString(), "ROLE_USER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void adminCanCharge() throws Exception {
		when(useCase.charge(any())).thenReturn(payment());

		mockMvc.perform(post(BASE).with(token(UUID.randomUUID().toString(), "ROLE_ADMIN"))
				.header("Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated());
	}

	@Test
	void serviceAccountCanCharge() throws Exception {
		when(useCase.charge(any())).thenReturn(payment());

		mockMvc.perform(post(BASE).with(serviceToken())
				.header("Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor token(String subject, String role) {
		return jwt().jwt(builder -> builder.subject(subject))
				.authorities(new SimpleGrantedAuthority(role));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor serviceToken() {
		return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())
				.claim("preferred_username", "service-account-casl-order-service")
				.claim("client_id", "casl-order-service"));
	}

	private static Payment payment() {
		return new Payment(UUID.randomUUID(), "key-1", new BigDecimal("19.98"), "USD",
				PaymentStatus.APPROVED, null, NOW, NOW);
	}
}
