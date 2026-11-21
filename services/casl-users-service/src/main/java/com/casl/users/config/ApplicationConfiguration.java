package com.casl.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Dependencias transversales. El reloj se inyecta para que los tests puedan
 * fijar el tiempo sin sleeps ni flakiness.
 */
@Configuration
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
