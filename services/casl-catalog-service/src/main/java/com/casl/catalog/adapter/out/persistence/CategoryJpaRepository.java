package com.casl.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
	Optional<CategoryJpaEntity> findByName(String name);
	boolean existsByName(String name);
}
