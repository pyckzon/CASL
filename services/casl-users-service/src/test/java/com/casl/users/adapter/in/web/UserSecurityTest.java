package com.casl.users.adapter.in.web;

import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.port.in.UserUseCase;
import com.casl.users.config.SecurityConfig;
import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.PhoneNumber;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserIdentity;
import com.casl.users.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de autorización del servicio: deny-by-default (401),
 * rol insuficiente (403), lectura del propio perfil y acceso admin.
 */
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserSecurityTest {

	private static final String BASE = "/api/users/v1/users";
	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000001");
	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
	private static final String VALID_BODY = """
			{"email":"Ana@Example.com","firstName":"Ana","lastName":"Pérez","phone":"+57 300 1234567"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get(BASE))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotListAllUsers() throws Exception {
		mockMvc.perform(get(BASE))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotReadAnotherUserById() throws Exception {
		mockMvc.perform(get(BASE + "/{id}", ID))
				.andExpect(status().isForbidden());

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanListUsers() throws Exception {
		when(useCase.list(any())).thenReturn(new PageResult<>(List.of(user()), 0, 20, 1));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk());
	}

	@Test
	void userCanReadOwnProfileBuiltFromTokenClaims() throws Exception {
		when(useCase.currentUser(any())).thenReturn(user());

		mockMvc.perform(get(BASE + "/me").with(jwt()
						.jwt(token -> token
								.subject("sub-1")
								.claim("email", "ana@example.com")
								.claim("email_verified", true)
								.claim("given_name", "Ana")
								.claim("family_name", "Pérez"))
						.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana@example.com"));

		ArgumentCaptor<IdentityProfile> captor = ArgumentCaptor.forClass(IdentityProfile.class);
		verify(useCase).currentUser(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new IdentityProfile(
				UserIdentity.KEYCLOAK, "sub-1", "ana@example.com", true, "Ana", "Pérez"));
	}

	@Test
	void anonymousCannotReadOwnProfile() throws Exception {
		mockMvc.perform(get(BASE + "/me"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotCreateResources() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanCreateResources() throws Exception {
		when(useCase.create(any())).thenReturn(user());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated());
	}

	private static User user() {
		return new User(ID, Email.of("ana@example.com"), "Ana", "Pérez", PhoneNumber.of("+57 300 1234567"),
				UserStatus.ACTIVE, CREATED, CREATED);
	}
}
