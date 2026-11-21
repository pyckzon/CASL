package com.casl.users.domain.port;

import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserFilter;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de persistencia. El dominio define qué necesita; el
 * adaptador decide cómo (JPA, memoria, otro almacén).
 */
public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByEmail(Email email);

	boolean existsByEmail(Email email);

	PageResult<User> findAll(UserFilter filter, int page, int size);

	void deleteById(UUID id);
}
