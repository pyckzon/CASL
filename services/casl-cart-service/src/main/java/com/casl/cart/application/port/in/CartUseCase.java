package com.casl.cart.application.port.in;

import com.casl.cart.application.command.AddItemCommand;
import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.command.RemoveItemCommand;
import com.casl.cart.application.command.UpdateQuantityCommand;
import com.casl.cart.domain.model.Cart;

import java.util.UUID;

public interface CartUseCase {
	Cart create(UUID userId);
	Cart get(UUID cartId);
	Cart addItem(AddItemCommand command);
	Cart updateQuantity(UpdateQuantityCommand command);
	Cart removeItem(RemoveItemCommand command);
	Cart clear(UUID cartId);
	Cart merge(MergeCartsCommand command);
}
