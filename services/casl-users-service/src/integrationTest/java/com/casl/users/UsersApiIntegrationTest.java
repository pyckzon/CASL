package com.casl.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración real: PostgreSQL en Testcontainers, migraciones Flyway y la API
 * completa. Verifica que el esquema, el mapeo JPA y el contrato coinciden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Testcontainers
class UsersApiIntegrationTest {

	private static final String BASE = "/api/users/v1/users";

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("DELETE FROM users");
	}

	@Test
	void fullUserLifecycleOverRealDatabase() throws Exception {
		String createBody = """
				{"email":"Ana@Example.com","firstName":"Ana","lastName":"Pérez","phone":"+57 300 1234567"}
				""";

		String location = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value("ana@example.com"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Ana"));

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));

		mockMvc.perform(get(BASE).param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].email").value("ana@example.com"));

		mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"ana.maria@example.com","firstName":"Ana María","lastName":"Pérez","phone":"+57 300 7654321","status":"INACTIVE"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andExpect(jsonPath("$.email").value("ana.maria@example.com"));

		mockMvc.perform(get(BASE).param("status", "INACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(delete(location))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(location))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void listAppliesPaginationAndFilters() throws Exception {		create("ana@example.com", "Ana");
		create("luis@example.com", "Luis");
		create("eva@example.com", "Eva");

		mockMvc.perform(get(BASE).param("page", "0").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get(BASE).param("email", "LUIS@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].firstName").value("Luis"));
	}

	@Test
	void legacyRouteIsGoneAndUnknownPathsReturn404ProblemJson() throws Exception {
		mockMvc.perform(get("/rest-api/v1/crud/user/ana").param("id", "123"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void firstAccessProvisionsExactlyOneUserFromToken() throws Exception {
		String firstId = mockMvc.perform(get(BASE + "/me").with(token("sub-jit", "nueva@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("nueva@example.com"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get(BASE + "/me").with(token("sub-jit", "nueva@example.com")))
				.andExpect(status().isOk())
				.andExpect(content().json(firstId));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void inactiveUserCannotReadOwnProfile() throws Exception {
		String location = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"inactivo@example.com","firstName":"Inés","lastName":"Pérez"}
						"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"inactivo@example.com","firstName":"Inés","lastName":"Pérez","status":"INACTIVE"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get(BASE + "/me").with(token("sub-inactive", "inactivo@example.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));
	}

	private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token(String subject, String email) {
		return jwt()
				.jwt(builder -> builder
						.subject(subject)
						.claim("email", email)
						.claim("email_verified", true)
						.claim("given_name", "Nombre")
						.claim("family_name", "Apellido"))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private void create(String email, String firstName) throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"%s","firstName":"%s","lastName":"Pérez"}
						""".formatted(email, firstName)))
				.andExpect(status().isCreated());
	}
}
