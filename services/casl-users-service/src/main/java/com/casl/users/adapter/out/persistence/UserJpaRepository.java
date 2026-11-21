package com.casl.users.adapter.out.persistence;

import com.casl.users.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data. Sus métodos derivados evitan JPQL dinámico: el
 * filtro opcional se resuelve eligiendo la consulta adecuada en el adaptador.
 */
interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

	Optional<UserJpaEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	Page<UserJpaEntity> findByStatus(UserStatus status, Pageable pageable);

	Page<UserJpaEntity> findByEmailEquals(String email, Pageable pageable);

	Page<UserJpaEntity> findByStatusAndEmail(UserStatus status, String email, Pageable pageable);
}
