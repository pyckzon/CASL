package com.casl.users.adapter.in.web;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserIdentity;
import org.springframework.security.oauth2.jwt.Jwt;

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

	/**
	 * Traduce los claims del token a un comando de aplicación. El dominio nunca
	 * ve JWT: aquí termina el anti-corruption de identidad.
	 */
	static IdentityProfile toProfile(Jwt jwt) {
		String preferredUsername = jwt.getClaimAsString("preferred_username");
		String email = jwt.getClaimAsString("email");
		String emailLocalPart = (email != null && email.contains("@"))
				? email.substring(0, email.indexOf('@'))
				: null;
		return new IdentityProfile(
				UserIdentity.KEYCLOAK,
				jwt.getSubject(),
				email,
				Boolean.TRUE.equals(jwt.getClaim("email_verified")),
				firstNonBlank(jwt.getClaimAsString("given_name"), preferredUsername, emailLocalPart, "User"),
				firstNonBlank(jwt.getClaimAsString("family_name"), preferredUsername, "User"));
	}

	private static String firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			if (candidate != null && !candidate.isBlank()) {
				return candidate.trim();
			}
		}
		throw new IllegalArgumentException("No se pudo derivar un nombre del token");
	}

	static UserPageResponse toResponse(PageResult<User> page) {
		List<UserResponse> content = page.content().stream()
				.map(UserWebMapper::toResponse)
				.toList();
		return new UserPageResponse(content, page.page(), page.size(), page.totalElements(), page.totalPages());
	}
}
