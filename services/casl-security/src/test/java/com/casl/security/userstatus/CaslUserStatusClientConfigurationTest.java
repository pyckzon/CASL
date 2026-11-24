package com.casl.security.userstatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CaslUserStatusClientConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(CaslUserStatusClientConfiguration.class);

	@Test
	void createsCachingRemoteVerifierWhenEnabled() {
		runner.withPropertyValues(
				"casl.security.user-status.enabled=true",
				"casl.security.user-status.base-url=http://localhost:8666",
				"casl.security.user-status.cache-ttl=60s",
				"casl.security.user-status.stale-grace=300s")
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

	@Test
	void defaultsComeFromProperties() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(UserStatusVerifier.class);
		});
	}
}
