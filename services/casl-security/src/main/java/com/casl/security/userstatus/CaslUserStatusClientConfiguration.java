package com.casl.security.userstatus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente del estado de usuario para los desplegables que no son dueños del
 * dato: el token del llamador se reenvía a {@code GET /api/users/v1/users/me/status}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CaslUserStatusProperties.class)
@ConditionalOnProperty(prefix = "casl.security.user-status", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaslUserStatusClientConfiguration {

	@Bean
	public UserStatusVerifier userStatusVerifier(CaslUserStatusProperties properties) {
		return new CachingUserStatusVerifier(new RemoteUserStatusVerifier(properties), properties);
	}
}
