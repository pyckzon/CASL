package com.casl.users.adapter.in.web;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.application.port.in.UserUseCase;
import com.casl.users.application.query.UserQuery;
import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.exception.UserNotFoundException;
import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.PhoneNumber;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

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

	@Test
	void createReturns201WithLocationAndResource() throws Exception {
		when(useCase.create(any())).thenReturn(user());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", BASE + "/" + ID))
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.email").value("ana@example.com"))
				.andExpect(jsonPath("$.firstName").value("Ana"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.createdAt").exists());

		verify(useCase).create(new CreateUserCommand("Ana@Example.com", "Ana", "Pérez", "+57 300 1234567"));
	}

	@Test
	void createWithInvalidBodyReturns400ProblemJson() throws Exception {
		String invalid = """
				{"email":"no-es-email","firstName":"","lastName":"Pérez"}
				""";

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.type").value("https://errors.casl.dev/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.instance").value(BASE))
				.andExpect(jsonPath("$.errors[0].field").value("email"))
				.andExpect(jsonPath("$.errors[1].field").value("firstName"));

		verifyNoInteractions(useCase);
	}

	@Test
	void createWithMalformedJsonReturns400ProblemJson() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void getReturnsUser() throws Exception {
		when(useCase.getById(ID)).thenReturn(user());

		mockMvc.perform(get(BASE + "/{id}", ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.phone").value("+57 300 1234567"));
	}

	@Test
	void getUnknownReturns404ProblemJson() throws Exception {
		when(useCase.getById(ID)).thenThrow(new UserNotFoundException(ID));

		mockMvc.perform(get(BASE + "/{id}", ID))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.type").value("https://errors.casl.dev/user-not-found"))
				.andExpect(jsonPath("$.instance").value(BASE + "/" + ID));
	}

	@Test
	void getWithInvalidUuidReturns400ProblemJson() throws Exception {
		mockMvc.perform(get(BASE + "/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void listReturnsPageMetadata() throws Exception {
		when(useCase.list(any())).thenReturn(new PageResult<>(List.of(user()), 0, 20, 1));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(ID.toString()))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		verify(useCase).list(new UserQuery(null, null, 0, 20));
	}

	@Test
	void listWithSizeAboveLimitReturns400ProblemJson() throws Exception {
		mockMvc.perform(get(BASE).param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(useCase);
	}

	@Test
	void updateReturnsUpdatedResource() throws Exception {
		User inactive = new User(ID, Email.of("ana.maria@example.com"), "Ana María", "Pérez",
				PhoneNumber.of("+57 300 7654321"), UserStatus.INACTIVE, CREATED, CREATED.plusSeconds(60));
		when(useCase.update(any(), any())).thenReturn(inactive);

		mockMvc.perform(put(BASE + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"ana.maria@example.com","firstName":"Ana María","lastName":"Pérez","status":"INACTIVE"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andExpect(jsonPath("$.email").value("ana.maria@example.com"));

		verify(useCase).update(ID,
				new UpdateUserCommand("ana.maria@example.com", "Ana María", "Pérez", null, "INACTIVE"));
	}

	@Test
	void updateWithTakenEmailReturns409ProblemJson() throws Exception {
		when(useCase.update(any(), any())).thenThrow(new EmailAlreadyExistsException("ana@example.com"));

		mockMvc.perform(put(BASE + "/{id}", ID).contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"ana@example.com","firstName":"Ana","lastName":"Pérez","status":"ACTIVE"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void deleteReturns204() throws Exception {
		mockMvc.perform(delete(BASE + "/{id}", ID))
				.andExpect(status().isNoContent());

		verify(useCase).delete(ID);
	}

	private static User user() {
		return new User(ID, Email.of("ana@example.com"), "Ana", "Pérez", PhoneNumber.of("+57 300 1234567"),
				UserStatus.ACTIVE, CREATED, CREATED);
	}
}
