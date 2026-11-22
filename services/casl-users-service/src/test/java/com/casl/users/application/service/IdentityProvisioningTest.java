package com.casl.users.application.service;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.exception.InactiveUserException;
import com.casl.users.domain.exception.InvalidUserException;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserFilter;
import com.casl.users.domain.model.UserIdentity;
import com.casl.users.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Anti-corruption de identidad: el token se traduce a un comando de aplicación
 * y el dominio resuelve usuario (vínculo existente, enlace por email verificado
 * o aprovisionamiento just-in-time).
 */
class IdentityProvisioningTest {

	private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");
	private static final String SUBJECT = "0b6b5d1e-1111-2222-3333-444455556666";

	private final InMemoryUserRepository repository = new InMemoryUserRepository();
	private final InMemoryUserIdentityRepository identities = new InMemoryUserIdentityRepository();
	private final UserApplicationService service =
			new UserApplicationService(repository, identities, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void firstAccessProvisionsUserAndLinksIdentity() {
		User user = service.currentUser(profile("ana@example.com", true));

		assertThat(user.email().value()).isEqualTo("ana@example.com");
		assertThat(user.firstName()).isEqualTo("Ana");
		assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(identities.findByProviderAndSubject(UserIdentity.KEYCLOAK, SUBJECT))
				.contains(new UserIdentity(user.id(), UserIdentity.KEYCLOAK, SUBJECT));
	}

	@Test
	void subsequentAccessReusesTheSameUser() {
		User first = service.currentUser(profile("ana@example.com", true));
		User second = service.currentUser(profile("ana@example.com", true));

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(repository.findAll(new UserFilter(null, null), 0, 10).totalElements()).isEqualTo(1);
	}

	@Test
	void verifiedEmailLinksToExistingUserInsteadOfDuplicating() {
		User existing = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		User resolved = service.currentUser(profile("ana@example.com", true));

		assertThat(resolved.id()).isEqualTo(existing.id());
		assertThat(repository.findAll(new UserFilter(null, null), 0, 10).totalElements()).isEqualTo(1);
	}

	@Test
	void unverifiedEmailCannotTakeOverAnExistingUser() {
		service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));

		assertThatThrownBy(() -> service.currentUser(profile("ana@example.com", false)))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	void unverifiedEmailWithFreeAddressStillProvisions() {
		User user = service.currentUser(profile("nueva@example.com", false));

		assertThat(user.email().value()).isEqualTo("nueva@example.com");
	}

	@Test
	void inactiveUserIsRejectedEvenWithValidIdentity() {
		User existing = service.create(new CreateUserCommand("ana@example.com", "Ana", "Pérez", null));
		service.update(existing.id(),
				new UpdateUserCommand("ana@example.com", "Ana", "Pérez", null, "INACTIVE"));

		assertThatThrownBy(() -> service.currentUser(profile("ana@example.com", true)))
				.isInstanceOf(InactiveUserException.class);
	}

	@Test
	void tokenWithoutEmailIsRejected() {
		assertThatThrownBy(() -> service.currentUser(new IdentityProfile(UserIdentity.KEYCLOAK, SUBJECT,
				null, false, "Ana", "Pérez")))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void deletingUserRemovesIdentityLink() {
		User user = service.currentUser(profile("ana@example.com", true));

		service.delete(user.id());

		assertThat(identities.findByProviderAndSubject(UserIdentity.KEYCLOAK, SUBJECT)).isEmpty();
	}

	private static IdentityProfile profile(String email, boolean verified) {
		return new IdentityProfile(UserIdentity.KEYCLOAK, SUBJECT, email, verified, "Ana", "Pérez");
	}
}
