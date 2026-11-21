package com.casl.users.adapter.out.persistence;

import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserFilter;
import com.casl.users.domain.port.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador que implementa el puerto {@link UserRepository} sobre PostgreSQL.
 * Traduce las violaciones de la restricción única de email a una excepción de
 * dominio, para que la capa de aplicación no conozca Spring Data.
 */
@Repository
public class UserPersistenceAdapter implements UserRepository {

	private final UserJpaRepository repository;

	public UserPersistenceAdapter(UserJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public User save(User user) {
		try {
			UserJpaEntity saved = repository.saveAndFlush(UserPersistenceMapper.toEntity(user));
			return UserPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw translateIntegrityViolation(exception, user.email());
		}
	}

	@Override
	public Optional<User> findById(UUID id) {
		return repository.findById(id).map(UserPersistenceMapper::toDomain);
	}

	@Override
	public Optional<User> findByEmail(Email email) {
		return repository.findByEmail(email.value()).map(UserPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByEmail(Email email) {
		return repository.existsByEmail(email.value());
	}

	@Override
	public PageResult<User> findAll(UserFilter filter, int page, int size) {
		Pageable pageable = UserPersistenceMapper.toPageable(page, size);
		Page<UserJpaEntity> result = query(filter, pageable);
		return UserPersistenceMapper.toPageResult(result);
	}

	@Override
	public void deleteById(UUID id) {
		repository.deleteById(id);
	}

	private Page<UserJpaEntity> query(UserFilter filter, Pageable pageable) {
		if (filter.status() != null && filter.email() != null) {
			return repository.findByStatusAndEmail(filter.status(), filter.email().value(), pageable);
		}
		if (filter.status() != null) {
			return repository.findByStatus(filter.status(), pageable);
		}
		if (filter.email() != null) {
			return repository.findByEmailEquals(filter.email().value(), pageable);
		}
		return repository.findAll(pageable);
	}

	private static RuntimeException translateIntegrityViolation(DataIntegrityViolationException exception, Email email) {
		String message = String.valueOf(exception.getMostSpecificCause().getMessage());
		if (message.contains("users_email") || message.contains("email")) {
			return new EmailAlreadyExistsException(email.value());
		}
		return exception;
	}
}
