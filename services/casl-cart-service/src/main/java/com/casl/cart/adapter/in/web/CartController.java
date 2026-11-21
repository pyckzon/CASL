package com.casl.cart.adapter.in.web;

import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.domain.model.Cart;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart/v1/carts")
public class CartController {

	private static final String BASE = "/api/cart/v1/carts/";

	private final CartUseCase useCase;

	public CartController(CartUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<CartResponse> create(@RequestParam(required = false) UUID userId) {
		Cart created = useCase.create(userId);
		return ResponseEntity.created(URI.create(BASE + created.cartId()))
				.body(CartWebMapper.toResponse(created));
	}

	@GetMapping("/{cartId}")
	public CartResponse get(@PathVariable UUID cartId) {
		return CartWebMapper.toResponse(useCase.get(cartId));
	}

	@PostMapping("/{cartId}/items")
	public CartResponse addItem(@PathVariable UUID cartId, @Valid @RequestBody AddItemRequest request) {
		return CartWebMapper.toResponse(useCase.addItem(CartWebMapper.toAddCommand(cartId, request)));
	}

	@PutMapping("/{cartId}/items/{productId}")
	public CartResponse updateQuantity(@PathVariable UUID cartId, @PathVariable UUID productId,
			@Valid @RequestBody UpdateQuantityRequest request) {
		return CartWebMapper.toResponse(useCase.updateQuantity(CartWebMapper.toUpdateCommand(cartId, productId, request)));
	}

	@DeleteMapping("/{cartId}/items/{productId}")
	public CartResponse removeItem(@PathVariable UUID cartId, @PathVariable UUID productId) {
		return CartWebMapper.toResponse(useCase.removeItem(new com.casl.cart.application.command.RemoveItemCommand(cartId, productId)));
	}

	@DeleteMapping("/{cartId}")
	public CartResponse clear(@PathVariable UUID cartId) {
		return CartWebMapper.toResponse(useCase.clear(cartId));
	}

	@PostMapping("/{cartId}/merge")
	public CartResponse merge(@PathVariable UUID cartId, @Valid @RequestBody MergeCartRequest request) {
		return CartWebMapper.toResponse(useCase.merge(new MergeCartsCommand(cartId, request.sourceCartId())));
	}
}
