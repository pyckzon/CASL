package com.casl.cart.adapter.out.persistence;

import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.port.CartRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class RedisCartRepository implements CartRepository {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final Duration ttl;

	public RedisCartRepository(StringRedisTemplate redis, ObjectMapper objectMapper,
			@Value("${casl.cart.ttl-minutes:1440}") long ttlMinutes) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.ttl = Duration.ofMinutes(ttlMinutes);
	}

	@Override
	public Cart save(Cart cart) {
		try {
			String json = objectMapper.writeValueAsString(cart);
			redis.opsForValue().set(key(cart.cartId()), json, ttl);
			return cart;
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to serialize cart " + cart.cartId(), exception);
		}
	}

	@Override
	public Optional<Cart> findById(UUID cartId) {
		String json = redis.opsForValue().get(key(cartId));
		if (json == null || json.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, Cart.class));
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to deserialize cart " + cartId, exception);
		}
	}

	@Override
	public void deleteById(UUID cartId) {
		redis.delete(key(cartId));
	}

	private static String key(UUID cartId) {
		return "cart:" + cartId;
	}
}
