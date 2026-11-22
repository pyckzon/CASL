package com.casl.users.domain.port;

import com.casl.users.domain.model.UserIdentity;

import java.util.Optional;
import java.util.UUID;

/**
 * Vínculos identidad externa ↔ usuario local. La combinación
 * (provider, subject) es única: un sujeto solo puede apuntar a un usuario.
 */
public interface UserIdentityRepository {

	Optional<UserIdentity> findByProviderAndSubject(String provider, String subject);

	UserIdentity save(UserIdentity identity);

	void deleteByUserId(UUID userId);
}
