package com.casl.users.adapter.in.web;

import com.casl.security.userstatus.UserStatus;
import com.casl.security.userstatus.UserStatusVerifier;
import com.casl.users.application.port.in.UserUseCase;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Verificador local del estado de usuario en el servicio dueño del dato: sin
 * HTTP ni caché, la baja de un usuario es inmediata.
 */
public class LocalUserStatusVerifier implements UserStatusVerifier {

	private final UserUseCase userUseCase;

	public LocalUserStatusVerifier(UserUseCase userUseCase) {
		this.userUseCase = userUseCase;
	}

	@Override
	public UserStatus verify(Jwt jwt) {
		return UserStatus.valueOf(userUseCase.statusOf(UserWebMapper.toProfile(jwt)).name());
	}
}
