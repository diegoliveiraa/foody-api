package com.foody.api.order.service;

import com.foody.api.catalog.service.ProductReferenceLookup;
import com.foody.api.order.repository.OrderItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderedProductLookup implements ProductReferenceLookup {

	private final OrderItemRepository orderItems;

	public OrderedProductLookup(OrderItemRepository orderItems) {
		this.orderItems = orderItems;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isReferenced(UUID productId) {
		return orderItems.existsByProductId(productId);
	}

}
