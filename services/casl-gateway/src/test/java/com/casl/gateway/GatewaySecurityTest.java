package com.casl.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El borde rechaza peticiones sin token antes de enrutar al servicio
 * downstream, y devuelve el error con el contrato RFC 9457.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void requestWithoutTokenIsRejectedBeforeRouting() throws Exception {
		mockMvc.perform(get("/api/users/v1/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void healthEndpointStaysPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}
}
