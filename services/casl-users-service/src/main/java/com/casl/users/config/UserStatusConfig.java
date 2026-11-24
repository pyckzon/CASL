package com.casl.users.config;

import com.casl.security.userstatus.UserStatusVerifier;
import com.casl.users.adapter.in.web.LocalUserStatusVerifier;
import com.casl.users.application.port.in.UserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El servicio de usuarios es la fuente de verdad del estado: el verificador
 * local consulta el caso de uso (con JIT) sin caché ni HTTP.
 */
@Configuration(proxyBeanMethods = false)
public class UserStatusConfig {

	@Bean
	public UserStatusVerifier userStatusVerifier(UserUseCase userUseCase) {
		return new LocalUserStatusVerifier(userUseCase);
	}
}
