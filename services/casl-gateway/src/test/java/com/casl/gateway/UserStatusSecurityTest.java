package com.casl.gateway;

import com.casl.security.userstatus.UserStatus;
import com.casl.security.userstatus.UserStatusVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El borde comprueba el estado del usuario en todos los endpoints protegidos
 * antes de reenviar (ADR-018).
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserStatusSecurityTest {

	private static final String PROTECTED_PATH = "/api/users/v1/users";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserStatusVerifier userStatusVerifier;

	@Test
	void inactiveUserWithUserRoleGetsUserStatusForbidden() throws Exception {
		given(userStatusVerifier.verify(any(Jwt.class))).willReturn(UserStatus.INACTIVE);

		mockMvc.perform(get(PROTECTED_PATH).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));
	}

	@Test
	void inactiveUserWithAdminRoleGetsUserStatusForbidden() throws Exception {
		given(userStatusVerifier.verify(any(Jwt.class))).willReturn(UserStatus.INACTIVE);

		mockMvc.perform(get(PROTECTED_PATH).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));
	}
}
