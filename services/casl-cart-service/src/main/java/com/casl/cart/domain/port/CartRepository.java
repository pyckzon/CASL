package com.casl.cart.domain.port;

import com.casl.cart.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
	Cart save(Cart cart);
	Optional<Cart> findById(UUID cartId);
	void deleteById(UUID cartId);
}
