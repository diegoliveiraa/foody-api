package com.foody.api.catalog.service;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CartItem;
import com.foody.api.catalog.dto.CartQuote;
import com.foody.api.catalog.dto.QuotedItem;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartPricingService {

	private final ProductRepository products;

	public CartPricingService(ProductRepository products) {
		this.products = products;
	}

	@Transactional(readOnly = true)
	public CartQuote quote(List<CartItem> items) {
		Set<UUID> productIds = items.stream().map(CartItem::productId).collect(Collectors.toSet());
		Map<UUID, Product> productsById = products.findAllWithCategoryByIdIn(productIds)
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));
		List<QuotedItem> quoted = items.stream().map(item -> quoteOf(item, productsById)).toList();
		BigDecimal total = quoted.stream()
			.filter(QuotedItem::available)
			.map(QuotedItem::subtotal)
			.reduce(CatalogConstants.ZERO_PRICE, BigDecimal::add);
		return new CartQuote(quoted, total);
	}

	private QuotedItem quoteOf(CartItem item, Map<UUID, Product> productsById) {
		return Optional.ofNullable(productsById.get(item.productId()))
			.map(product -> price(item, product))
			.orElseGet(() -> QuotedItem.missing(item));
	}

	private QuotedItem price(CartItem item, Product product) {
		if (!product.isAvailable()) {
			return QuotedItem.unavailable(item, product);
		}
		BigDecimal subtotal = product.getPrice()
			.multiply(BigDecimal.valueOf(item.quantity()))
			.setScale(CatalogConstants.PRICE_SCALE, CatalogConstants.PRICE_ROUNDING);
		return QuotedItem.available(item, product, subtotal);
	}

}
