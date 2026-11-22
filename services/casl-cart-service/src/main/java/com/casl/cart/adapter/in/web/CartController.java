package com.casl.cart.adapter.in.web;

import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.command.RemoveItemCommand;
import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.domain.model.Cart;
import com.casl.security.JwtSubject;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart/v1/carts")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class CartController {

	private static final String BASE = "/api/cart/v1/carts/";

	private final CartUseCase useCase;

	public CartController(CartUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<CartResponse> create(@AuthenticationPrincipal Jwt jwt) {
		Cart created = useCase.create(JwtSubject.requireUuid(jwt));
		return ResponseEntity.created(URI.create(BASE + created.cartId()))
				.body(CartWebMapper.toResponse(created));
	}

	@GetMapping("/{cartId}")
	public CartResponse get(@PathVariable UUID cartId, @AuthenticationPrincipal Jwt jwt) {
		return CartWebMapper.toResponse(owned(cartId, jwt));
	}

	@PostMapping("/{cartId}/items")
	public CartResponse addItem(@PathVariable UUID cartId, @Valid @RequestBody AddItemRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		owned(cartId, jwt);
		return CartWebMapper.toResponse(useCase.addItem(CartWebMapper.toAddCommand(cartId, request)));
	}

	@PutMapping("/{cartId}/items/{productId}")
	public CartResponse updateQuantity(@PathVariable UUID cartId, @PathVariable UUID productId,
			@Valid @RequestBody UpdateQuantityRequest request, @AuthenticationPrincipal Jwt jwt) {
		owned(cartId, jwt);
		return CartWebMapper.toResponse(useCase.updateQuantity(CartWebMapper.toUpdateCommand(cartId, productId, request)));
	}

	@DeleteMapping("/{cartId}/items/{productId}")
	public CartResponse removeItem(@PathVariable UUID cartId, @PathVariable UUID productId,
			@AuthenticationPrincipal Jwt jwt) {
		owned(cartId, jwt);
		return CartWebMapper.toResponse(useCase.removeItem(new RemoveItemCommand(cartId, productId)));
	}

	@DeleteMapping("/{cartId}")
	public CartResponse clear(@PathVariable UUID cartId, @AuthenticationPrincipal Jwt jwt) {
		owned(cartId, jwt);
		return CartWebMapper.toResponse(useCase.clear(cartId));
	}

	@PostMapping("/{cartId}/merge")
	public CartResponse merge(@PathVariable UUID cartId, @Valid @RequestBody MergeCartRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		owned(cartId, jwt);
		owned(request.sourceCartId(), jwt);
		return CartWebMapper.toResponse(useCase.merge(new MergeCartsCommand(cartId, request.sourceCartId())));
	}

	/** Verifica la propiedad antes de cualquier operación sobre el carrito. */
	private Cart owned(UUID cartId, Jwt jwt) {
		Cart cart = useCase.get(cartId);
		JwtSubject.requireOwner(cart.userId(), jwt);
		return cart;
	}
}
