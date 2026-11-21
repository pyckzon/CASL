package com.casl.users.application.service;

import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserFilter;
import com.casl.users.domain.port.UserRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio en memoria para probar los casos de uso sin Spring ni base de
 * datos. Implementa el mismo puerto que el adaptador de producción.
 */
class InMemoryUserRepository implements UserRepository {

	private final Map<UUID, User> users = new LinkedHashMap<>();

	@Override
	public User save(User user) {
		users.put(user.id(), user);
		return user;
	}

	@Override
	public Optional<User> findById(UUID id) {
		return Optional.ofNullable(users.get(id));
	}

	@Override
	public Optional<User> findByEmail(Email email) {
		return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
	}

	@Override
	public boolean existsByEmail(Email email) {
		return findByEmail(email).isPresent();
	}

	@Override
	public PageResult<User> findAll(UserFilter filter, int page, int size) {
		List<User> filtered = users.values().stream()
				.filter(user -> filter.status() == null || user.status() == filter.status())
				.filter(user -> filter.email() == null || user.email().equals(filter.email()))
				.sorted(Comparator.comparing(User::createdAt).reversed())
				.toList();
		int from = Math.min(page * size, filtered.size());
		int to = Math.min(from + size, filtered.size());
		return new PageResult<>(filtered.subList(from, to), page, size, filtered.size());
	}

	@Override
	public void deleteById(UUID id) {
		users.remove(id);
	}
}
