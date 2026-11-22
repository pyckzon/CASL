package com.casl.users.application.service;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.application.port.in.UserUseCase;
import com.casl.users.application.query.UserQuery;
import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.exception.InactiveUserException;
import com.casl.users.domain.exception.InvalidUserException;
import com.casl.users.domain.exception.UserNotFoundException;
import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.PhoneNumber;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserFilter;
import com.casl.users.domain.model.UserIdentity;
import com.casl.users.domain.model.UserStatus;
import com.casl.users.domain.port.UserIdentityRepository;
import com.casl.users.domain.port.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de los casos de uso. Orquesta dominio y puertos: no conoce
 * HTTP, JPA ni el formato de los errores de la API.
 */
@Log4j2
@Service
@Transactional(readOnly = true)
public class UserApplicationService implements UserUseCase {

	private final UserRepository repository;
	private final UserIdentityRepository identities;
	private final Clock clock;

	public UserApplicationService(UserRepository repository, UserIdentityRepository identities, Clock clock) {
		this.repository = repository;
		this.identities = identities;
		this.clock = clock;
	}

	@Override
	@Transactional
	public User create(CreateUserCommand command) {
		Email email = Email.of(command.email());
		if (repository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException(email.value());
		}
		Instant now = now();
		User user = new User(
				UUID.randomUUID(),
				email,
				command.firstName(),
				command.lastName(),
				PhoneNumber.of(command.phone()),
				UserStatus.ACTIVE,
				now,
				now);
		User saved = repository.save(user);
		log.info("User created id={} email={}", saved.id(), saved.email().value());
		return saved;
	}

	@Override
	public User getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
	}

	@Override
	public PageResult<User> list(UserQuery query) {
		Email email = normalize(query.email());
		UserFilter filter = new UserFilter(query.status(), email);
		return repository.findAll(filter, query.page(), query.size());
	}

	@Override
	@Transactional
	public User currentUser(IdentityProfile profile) {
		User user = identities.findByProviderAndSubject(profile.provider(), profile.subject())
				.flatMap(identity -> repository.findById(identity.userId()))
				.orElseGet(() -> provision(profile));
		if (user.status() != UserStatus.ACTIVE) {
			throw new InactiveUserException(user.id());
		}
		return user;
	}

	@Override
	@Transactional
	public User update(UUID id, UpdateUserCommand command) {
		User current = getById(id);
		Email email = Email.of(command.email());
		if (!current.email().equals(email) && repository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException(email.value());
		}
		User updated = current.update(
				email,
				command.firstName(),
				command.lastName(),
				PhoneNumber.of(command.phone()),
				parseStatus(command.status()),
				now());
		User saved = repository.save(updated);
		log.info("User updated id={} email={}", saved.id(), saved.email().value());
		return saved;
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		User user = getById(id);
		identities.deleteByUserId(user.id());
		repository.deleteById(user.id());
		log.info("User deleted id={}", user.id());
	}

	/**
	 * Enlaza la identidad con un usuario existente solo si el proveedor verificó
	 * el email; en caso contrario aprovisiona un usuario nuevo. Así un token con
	 * un email no verificado no puede apropiarse de un perfil ajeno.
	 */
	private User provision(IdentityProfile profile) {
		if (profile.email() == null || profile.email().isBlank()) {
			throw new InvalidUserException("email is required");
		}
		Email email = Email.of(profile.email());
		User user = (profile.emailVerified() ? repository.findByEmail(email) : Optional.<User>empty())
				.orElseGet(() -> createFromIdentity(email, profile));
		return link(user, profile);
	}

	private User createFromIdentity(Email email, IdentityProfile profile) {
		if (repository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException(email.value());
		}
		Instant now = now();
		User created = repository.save(new User(
				UUID.randomUUID(),
				email,
				profile.firstName(),
				profile.lastName(),
				new PhoneNumber(null),
				UserStatus.ACTIVE,
				now,
				now));
		log.info("User provisioned from identity id={} provider={}", created.id(), profile.provider());
		return created;
	}

	private User link(User user, IdentityProfile profile) {
		// Si dos peticiones aprovisionan el mismo sujeto a la vez, la restricción
		// única (provider, subject) deja ganar a una y la otra recibe 409
		// IDENTITY_ALREADY_LINKED; reintentar resuelve el vínculo ganador.
		identities.save(new UserIdentity(user.id(), profile.provider(), profile.subject()));
		log.info("Identity linked userId={} provider={}", user.id(), profile.provider());
		return user;
	}

	/**
	 * PostgreSQL {@code TIMESTAMPTZ} guarda microsegundos: truncar aquí evita que
	 * la respuesta de la creación y el GET posterior difieran en nanosegundos.
	 */
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

	private static Email normalize(String email) {
		return (email == null || email.isBlank()) ? null : Email.of(email);
	}

	private static UserStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			throw new InvalidUserException("status is required");
		}
		try {
			return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new InvalidUserException("status must be ACTIVE or INACTIVE");
		}
	}
}
