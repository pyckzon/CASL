package com.casl.users.application.service;

import com.casl.users.domain.exception.IdentityAlreadyExistsException;
import com.casl.users.domain.model.UserIdentity;
import com.casl.users.domain.port.UserIdentityRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación en memoria del puerto de identidades, con la misma restricción
 * única (provider, subject) que la base de datos.
 */
class InMemoryUserIdentityRepository implements UserIdentityRepository {

	private final Map<String, UserIdentity> identities = new LinkedHashMap<>();

	@Override
	public Optional<UserIdentity> findByProviderAndSubject(String provider, String subject) {
		return Optional.ofNullable(identities.get(key(provider, subject)));
	}

	@Override
	public UserIdentity save(UserIdentity identity) {
		String key = key(identity.provider(), identity.subject());
		if (identities.containsKey(key)) {
			throw new IdentityAlreadyExistsException(identity.subject());
		}
		identities.put(key, identity);
		return identity;
	}

	@Override
	public void deleteByUserId(UUID userId) {
		identities.values().removeIf(identity -> identity.userId().equals(userId));
	}

	private static String key(String provider, String subject) {
		return provider + "|" + subject;
	}
}
