package com.casl.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.casl.security.userstatus.UserStatusVerifier;
import com.casl.security.userstatus.CachingUserStatusVerifier;

/**
 * El consumidor Kafka delega en el cliente remoto del estado del usuario
 * (ADR-018): sin controller HTTP, la verificación la aporta la configuración.
 */
class UserStatusConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(UserStatusConfig.class);

	@Test
	void createsCachingRemoteVerifierWhenEnabled() {
		runner.withPropertyValues(
				"casl.security.user-status.enabled=true",
				"casl.security.user-status.base-url=http://localhost:8666")
				.run(context -> {
					assertThat(context).hasSingleBean(UserStatusVerifier.class);
					assertThat(context.getBean(UserStatusVerifier.class)).isInstanceOf(CachingUserStatusVerifier.class);
				});
	}

	@Test
	void doesNotCreateVerifierWhenDisabled() {
		runner.withPropertyValues("casl.security.user-status.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(UserStatusVerifier.class));
	}
}
