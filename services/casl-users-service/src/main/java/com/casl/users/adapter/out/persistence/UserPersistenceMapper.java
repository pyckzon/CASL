package com.casl.users.adapter.out.persistence;

import com.casl.users.domain.model.Email;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.PhoneNumber;
import com.casl.users.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Traducción entre el agregado de dominio y la entidad JPA. Es el único punto
 * que conoce ambos mundos.
 */
final class UserPersistenceMapper {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

	private UserPersistenceMapper() {
	}

	static UserJpaEntity toEntity(User user) {
		return new UserJpaEntity(
				user.id(),
				user.email().value(),
				user.firstName(),
				user.lastName(),
				user.phone().value(),
				user.status(),
				user.createdAt(),
				user.updatedAt());
	}

	static User toDomain(UserJpaEntity entity) {
		return new User(
				entity.getId(),
				new Email(entity.getEmail()),
				entity.getFirstName(),
				entity.getLastName(),
				new PhoneNumber(entity.getPhone()),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	static Pageable toPageable(int page, int size) {
		return PageRequest.of(page, size, NEWEST_FIRST);
	}

	static PageResult<User> toPageResult(Page<UserJpaEntity> page) {
		List<User> content = page.getContent().stream()
				.map(UserPersistenceMapper::toDomain)
				.toList();
		return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
	}
}
