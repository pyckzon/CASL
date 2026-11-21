package com.casl.cart.config;

import com.casl.cart.domain.port.CartRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	CartRepository cartRepository(StringRedisTemplate redis, ObjectMapper objectMapper,
			@Value("${casl.cart.ttl-minutes:1440}") long ttlMinutes) {
		return new com.casl.cart.adapter.out.persistence.RedisCartRepository(redis, objectMapper, ttlMinutes);
	}
}
