package com.casl.users.application.service;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.application.query.UserQuery;
import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.exception.InvalidUserException;
import com.casl.users.domain.exception.UserNotFoundException;
import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(3600);

	private final InMemoryUserRepository repository = new InMemoryUserRepository();
	private final UserApplicationService service = serviceAt(NOW);

	@Test
	void createNormalizesDataAndAssignsServerValues() {
		User user = service.create(new CreateUserCommand(" Ana@Example.COM ", " Ana ", " Pérez ", "+57 300 1234567"));

		assertThat(user.id()).isNotNull();
		assertThat(user.email().value()).isEqualTo("ana@example.com");
		assertThat(user.firstName()).isEqualTo("Ana");
		assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.createdAt()).isEqualTo(NOW);
		assertThat(user.updatedAt()).isEqualTo(NOW);
		assertThat(repository.findById(user.id())).contains(user);
	}

	@Test
	void createRejectsDuplicateEmailIgnoringCase() {
		service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		assertThatThrownBy(() -> service.create(new CreateUserCommand("ANA@example.com", "Otra", "Persona", null)))
				.isInstanceOf(EmailAlreadyExistsException.class)
				.hasMessageContaining("ana@example.com");
	}

	@Test
	void createRejectsInvalidData() {
		assertThatThrownBy(() -> service.create(new CreateUserCommand("no-es-email", "Ana", "Pérez", null)))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void getByIdReturnsUserOrThrows() {
		User created = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		assertThat(service.getById(created.id())).isEqualTo(created);

		UUID unknown = UUID.randomUUID();
		assertThatThrownBy(() -> service.getById(unknown))
				.isInstanceOf(UserNotFoundException.class)
				.hasMessageContaining(unknown.toString());
	}

	@Test
	void listFiltersAndPaginates() {
		service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));
		service.create(new CreateUserCommand("luis@example.com", "Luis", "Gómez", null));
		User inactive = service.create(new CreateUserCommand("eva@example.com", "Eva", "Díaz", null));
		service.update(inactive.id(), new UpdateUserCommand("eva@example.com", "Eva", "Díaz", null, "INACTIVE"));

		PageResult<User> firstPage = service.list(new UserQuery(null, null, 0, 2));
		PageResult<User> activeOnly = service.list(new UserQuery(UserStatus.ACTIVE, null, 0, 10));
		PageResult<User> byEmail = service.list(new UserQuery(null, "LUIS@example.com", 0, 10));

		assertThat(firstPage.content()).hasSize(2);
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(activeOnly.content()).extracting(User::email)
				.containsExactlyInAnyOrder(Email.of("ana@example.com"), Email.of("luis@example.com"));
		assertThat(byEmail.content()).singleElement().extracting(User::firstName).isEqualTo("Luis");
	}

	@Test
	void updateReplacesFieldsAndAdvancesTimestamp() {
		User created = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		User updated = serviceAt(LATER).update(created.id(),
				new UpdateUserCommand("ana.maria@example.com", "Ana María", "Pérez", "+57 300 7654321", "INACTIVE"));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.createdAt()).isEqualTo(NOW);
		assertThat(updated.updatedAt()).isEqualTo(LATER);
		assertThat(updated.email().value()).isEqualTo("ana.maria@example.com");
		assertThat(updated.firstName()).isEqualTo("Ana María");
		assertThat(updated.status()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	void updateRejectsEmailUsedByAnotherUser() {
		service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));
		User luis = service.create(new CreateUserCommand("luis@example.com", "Luis", "Gómez", null));

		assertThatThrownBy(() -> service.update(luis.id(),
				new UpdateUserCommand("ana@example.com", "Luis", "Gómez", null, "ACTIVE")))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	void updateAllowsKeepingOwnEmail() {
		User created = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		User updated = serviceAt(LATER).update(created.id(),
				new UpdateUserCommand("ana@example.com", "Ana", "Pérez Gómez", null, "ACTIVE"));

		assertThat(updated.lastName()).isEqualTo("Pérez Gómez");
	}

	@Test
	void updateRejectsInvalidStatus() {
		User created = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		assertThatThrownBy(() -> service.update(created.id(),
				new UpdateUserCommand("ana@example.com", "Ana", "Pérez", null, "DELETED")))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("ACTIVE or INACTIVE");
	}

	@Test
	void deleteRemovesUserAndFailsForUnknownId() {
		User created = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		service.delete(created.id());

		assertThat(repository.findById(created.id())).isEmpty();
		assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(UserNotFoundException.class);
	}

	private UserApplicationService serviceAt(Instant instant) {
		return new UserApplicationService(repository, Clock.fixed(instant, ZoneOffset.UTC));
	}
}
