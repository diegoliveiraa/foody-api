package com.foody.api.catalog.service;

import com.foody.api.catalog.dto.MenuCategory;
import com.foody.api.catalog.dto.MenuResponse;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

	private final ProductRepository products;

	public MenuService(ProductRepository products) {
		this.products = products;
	}

	@Transactional(readOnly = true)
	public MenuResponse menu() {
		Map<Category, List<Product>> productsByCategory = products.findAllOnMenu()
			.stream()
			.collect(Collectors.groupingBy(Product::getCategory, LinkedHashMap::new, Collectors.toList()));
		List<MenuCategory> categories = productsByCategory.entrySet()
			.stream()
			.map(entry -> MenuCategory.of(entry.getKey(), entry.getValue()))
			.toList();
		return new MenuResponse(categories);
	}

}
