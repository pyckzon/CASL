package com.casl.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagementPortSecurityTest.TestApp.class, properties = "management.server.port=9090")
@AutoConfigureMockMvc
class ManagementPortSecurityTest {

	private static final int MANAGEMENT_PORT = 9090;

	private static final int APPLICATION_PORT = 8080;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void managementPortAllowsActuatorEndpointsAndDeniesArbitraryRoutes() throws Exception {
		mockMvc.perform(get("/actuator/health").with(request -> {
			request.setLocalPort(MANAGEMENT_PORT);
			return request;
		})).andExpect(status().isOk());

		mockMvc.perform(get("/actuator/metrics").with(request -> {
			request.setLocalPort(MANAGEMENT_PORT);
			return request;
		})).andExpect(status().isOk());

		mockMvc.perform(get("/actuator/prometheus").with(request -> {
			request.setLocalPort(MANAGEMENT_PORT);
			return request;
		})).andExpect(status().isOk());

		mockMvc.perform(get("/private").with(request -> {
			request.setLocalPort(MANAGEMENT_PORT);
			return request;
		})).andExpect(status().isUnauthorized());
	}

	@Test
	void applicationPortStillRequiresJwtForApiRoutes() throws Exception {
		mockMvc.perform(get("/api/test").with(request -> {
			request.setLocalPort(APPLICATION_PORT);
			return request;
		})).andExpect(status().isUnauthorized());
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import({ CaslSecurityConfiguration.class, PrometheusController.class, HealthController.class,
			MetricsController.class, ApiController.class, PrivateController.class })
	static class TestApp {

		@Bean
		JwtDecoder jwtDecoder() {
			return token -> Jwt.withTokenValue(token)
					.header("alg", "none")
					.claim("sub", "test-user")
					.build();
		}
	}

	@RestController
	static class PrometheusController {

		@GetMapping("/actuator/prometheus")
		String prometheus() {
			return "# HELP casl_up\n";
		}
	}

	@RestController
	static class HealthController {

		@GetMapping("/actuator/health")
		String health() {
			return "{\"status\":\"UP\"}";
		}
	}

	@RestController
	static class MetricsController {

		@GetMapping("/actuator/metrics")
		String metrics() {
			return "# metrics\n";
		}
	}

	@RestController
	static class ApiController {

		@GetMapping("/api/test")
		String api() {
			return "ok";
		}
	}

	@RestController
	static class PrivateController {

		@GetMapping("/private")
		String secret() {
			return "secret";
		}
	}
}
