package com.casl.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID>, JpaSpecificationExecutor<ProductJpaEntity> {
	Optional<ProductJpaEntity> findBySku(String sku);
	boolean existsBySku(String sku);
}
