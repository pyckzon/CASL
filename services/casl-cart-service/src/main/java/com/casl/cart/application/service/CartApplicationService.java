package com.casl.cart.application.service;

import com.casl.cart.application.command.AddItemCommand;
import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.command.RemoveItemCommand;
import com.casl.cart.application.command.UpdateQuantityCommand;
import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.domain.exception.CartNotFoundException;
import com.casl.cart.domain.exception.ProductNotAvailableException;
import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CartItem;
import com.casl.cart.domain.model.CatalogProduct;
import com.casl.cart.domain.port.CartRepository;
import com.casl.cart.domain.port.CatalogClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
public class CartApplicationService implements CartUseCase {

	private final CartRepository repository;
	private final CatalogClient catalogClient;
	private final Clock clock;

	public CartApplicationService(CartRepository repository, CatalogClient catalogClient, Clock clock) {
		this.repository = repository;
		this.catalogClient = catalogClient;
		this.clock = clock;
	}

	@Override
	public Cart create(UUID userId) {
		Cart cart = Cart.empty(UUID.randomUUID(), userId, clock.instant());
		Cart saved = repository.save(cart);
		log.info("Cart created id={} userId={}", saved.cartId(), saved.userId());
		return saved;
	}

	@Override
	public Cart get(UUID cartId) {
		return repository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));
	}

	@Override
	public Cart addItem(AddItemCommand command) {
		Cart cart = get(command.cartId());
		CatalogProduct product = resolveProduct(command.productId(), command.sku());
		if (!product.isAvailable(command.quantity())) {
			throw new ProductNotAvailableException(product.sku());
		}
		CartItem item = new CartItem(product.id(), product.sku(), product.name(), product.unitPrice(),
				product.currency(), command.quantity());
		Cart updated = cart.addItem(item);
		Cart saved = repository.save(updated);
		log.info("Item added cart={} sku={} qty={}", saved.cartId(), product.sku(), command.quantity());
		return saved;
	}

	@Override
	public Cart updateQuantity(UpdateQuantityCommand command) {
		Cart cart = get(command.cartId());
		Cart updated = cart.updateQuantity(command.productId(), command.quantity());
		return repository.save(updated);
	}

	@Override
	public Cart removeItem(RemoveItemCommand command) {
		Cart cart = get(command.cartId());
		Cart updated = cart.removeItem(command.productId());
		return repository.save(updated);
	}

	@Override
	public Cart clear(UUID cartId) {
		Cart cart = get(cartId);
		return repository.save(cart.clear());
	}

	@Override
	public Cart merge(MergeCartsCommand command) {
		Cart target = get(command.targetCartId());
		Cart source = get(command.sourceCartId());
		Cart merged = target.merge(source);
		Cart saved = repository.save(merged);
		repository.deleteById(source.cartId());
		log.info("Carts merged target={} source={}", target.cartId(), source.cartId());
		return saved;
	}

	private CatalogProduct resolveProduct(UUID productId, String sku) {
		if (productId != null) {
			return catalogClient.findById(productId)
					.orElseThrow(() -> new ProductNotAvailableException(String.valueOf(productId)));
		}
		if (sku != null && !sku.isBlank()) {
			return catalogClient.findBySku(sku)
					.orElseThrow(() -> new ProductNotAvailableException(sku));
		}
		throw new IllegalArgumentException("productId or sku is required");
	}
}
