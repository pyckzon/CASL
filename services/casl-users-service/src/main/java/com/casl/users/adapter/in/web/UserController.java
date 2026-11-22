package com.casl.users.adapter.in.web;

import com.casl.users.application.port.in.UserUseCase;
import com.casl.users.application.query.UserQuery;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;
import com.casl.users.domain.model.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Adaptador de entrada HTTP. No contiene reglas de negocio: valida forma,
 * delega en el caso de uso y traduce el resultado al contrato.
 */
@RestController
@RequestMapping("/api/users/v1/users")
public class UserController {

	private static final String RESOURCE_PATH = "/api/users/v1/users/";

	private final UserUseCase useCase;

	public UserController(UserUseCase useCase) {
		this.useCase = useCase;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public UserPageResponse list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(required = false) @Email @Size(max = 320) String email) {
		PageResult<User> result = useCase.list(new UserQuery(status, email, page, size));
		return UserWebMapper.toResponse(result);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public UserResponse get(@PathVariable UUID id) {
		return UserWebMapper.toResponse(useCase.getById(id));
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		User created = useCase.create(UserWebMapper.toCommand(request));
		return ResponseEntity.created(URI.create(RESOURCE_PATH + created.id()))
				.body(UserWebMapper.toResponse(created));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		return UserWebMapper.toResponse(useCase.update(id, UserWebMapper.toCommand(request)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		useCase.delete(id);
		return ResponseEntity.noContent().build();
	}
}
