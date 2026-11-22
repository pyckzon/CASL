package com.casl.users.application.port.in;

import com.casl.users.application.command.CreateUserCommand;
import com.casl.users.application.command.IdentityProfile;
import com.casl.users.application.command.UpdateUserCommand;
import com.casl.users.application.query.UserQuery;
import com.casl.users.domain.model.PageResult;
import com.casl.users.domain.model.User;

import java.util.UUID;

/**
 * Puerto de entrada del servicio de usuarios. El adaptador web depende de esta
 * interfaz, no de la implementación, de modo que se puede decorar (caché,
 * métricas, autorización) sin tocar el controlador.
 */
public interface UserUseCase {

	User create(CreateUserCommand command);

	User getById(UUID id);

	/**
	 * Resuelve el usuario del token: reutiliza el vínculo existente, enlaza por
	 * email verificado o aprovisiona un usuario nuevo (just-in-time).
	 */
	User currentUser(IdentityProfile profile);

	PageResult<User> list(UserQuery query);

	User update(UUID id, UpdateUserCommand command);

	void delete(UUID id);
}
