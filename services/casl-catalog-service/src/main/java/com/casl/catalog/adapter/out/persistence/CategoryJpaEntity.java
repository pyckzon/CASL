package com.casl.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100, unique = true)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CategoryJpaEntity() {
	}

	public CategoryJpaEntity(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
