package com.casl.users.adapter.in.web;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;

import java.util.List;

/**
 * Traducción entre el contrato HTTP y los casos de uso. Mantiene al controlador
 * sin lógica de mapeo.
 */
final class UserWebMapper {

	private UserWebMapper() {
	}

	static CreateUserCommand toCommand(CreateUserRequest request) {
		return new CreateUserCommand(request.email(), request.firstName(), request.lastName(), request.phone());
	}

	static UpdateUserCommand toCommand(UpdateUserRequest request) {
		return new UpdateUserCommand(
				request.email(),
				request.firstName(),
				request.lastName(),
				request.phone(),
				request.status().name());
	}

	static UserResponse toResponse(User user) {
		return new UserResponse(
				user.id(),
				user.email().value(),
				user.firstName(),
				user.lastName(),
				user.phone().value(),
				user.status(),
				user.createdAt(),
				user.updatedAt());
	}

	static UserPageResponse toResponse(PageResult<User> page) {
		List<UserResponse> content = page.content().stream()
				.map(UserWebMapper::toResponse)
				.toList();
		return new UserPageResponse(content, page.page(), page.size(), page.totalElements(), page.totalPages());
	}
}
