package com.casl.users.adapter.out.persistence;

import com.casl.users.domain.exception.IdentityAlreadyExistsException;
import com.casl.users.domain.model.UserIdentity;
import com.casl.users.domain.port.UserIdentityRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador del puerto {@link UserIdentityRepository}. La restricción única
 * (provider, subject) protege ante aprovisionamientos concurrentes; aquí se
 * traduce a una excepción de dominio.
 */
@Repository
public class UserIdentityPersistenceAdapter implements UserIdentityRepository {

	private final UserIdentityJpaRepository repository;

	public UserIdentityPersistenceAdapter(UserIdentityJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<UserIdentity> findByProviderAndSubject(String provider, String subject) {
		return repository.findByProviderAndSubject(provider, subject).map(UserIdentityPersistenceAdapter::toDomain);
	}

	@Override
	public UserIdentity save(UserIdentity identity) {
		try {
			UserIdentityJpaEntity saved = repository.saveAndFlush(new UserIdentityJpaEntity(
					UUID.randomUUID(),
					identity.userId(),
					identity.provider(),
					identity.subject()));
			return toDomain(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new IdentityAlreadyExistsException(identity.subject());
		}
	}

	@Override
	public void deleteByUserId(UUID userId) {
		repository.deleteByUserId(userId);
	}

	private static UserIdentity toDomain(UserIdentityJpaEntity entity) {
		return new UserIdentity(entity.getUserId(), entity.getProvider(), entity.getSubject());
	}
}
