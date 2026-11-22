package com.casl.users.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserIdentityJpaRepository extends JpaRepository<UserIdentityJpaEntity, UUID> {

	Optional<UserIdentityJpaEntity> findByProviderAndSubject(String provider, String subject);

	void deleteByUserId(UUID userId);
}
