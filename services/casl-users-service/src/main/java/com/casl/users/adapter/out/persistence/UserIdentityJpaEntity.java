package com.casl.users.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entidad de persistencia del vínculo identidad ↔ usuario. Nunca sale del
 * adaptador (ADR-003).
 */
@Entity
@Table(name = "user_identities")
public class UserIdentityJpaEntity {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 40)
	private String provider;

	@Column(nullable = false, length = 100)
	private String subject;

	protected UserIdentityJpaEntity() {
		// Requerido por JPA.
	}

	public UserIdentityJpaEntity(UUID id, UUID userId, String provider, String subject) {
		this.id = id;
		this.userId = userId;
		this.provider = provider;
		this.subject = subject;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getProvider() {
		return provider;
	}

	public String getSubject() {
		return subject;
	}
}
