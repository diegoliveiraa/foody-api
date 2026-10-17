package com.foody.api.catalog.service;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CategoryProductCount;
import com.foody.api.catalog.dto.CategoryRequest;
import com.foody.api.catalog.dto.CategoryResponse;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.exception.CategoryInUseException;
import com.foody.api.catalog.exception.CategoryNameTakenException;
import com.foody.api.catalog.exception.CategoryNotFoundException;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.catalog.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final CategoryRepository categories;

	private final ProductRepository products;

	public CategoryService(CategoryRepository categories, ProductRepository products) {
		this.categories = categories;
		this.products = products;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list() {
		Map<UUID, Long> productCounts = products.countPerCategory()
			.stream()
			.collect(Collectors.toMap(CategoryProductCount::categoryId, CategoryProductCount::productCount));
		return categories.findAll(CatalogConstants.CATEGORY_SORT)
			.stream()
			.map(category -> CategoryResponse.of(category,
					productCounts.getOrDefault(category.getId(), CatalogConstants.NO_PRODUCTS)))
			.toList();
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categories.existsByName(request.name())) {
			throw new CategoryNameTakenException();
		}
		Category category = categories.save(new Category(request.name(), request.displayOrder()));
		return CategoryResponse.of(category, CatalogConstants.NO_PRODUCTS);
	}

	@Transactional
	public CategoryResponse update(UUID id, CategoryRequest request) {
		Category category = find(id);
		if (categories.existsByNameOnOtherThan(request.name(), id)) {
			throw new CategoryNameTakenException();
		}
		category.update(request.name(), request.displayOrder());
		return describe(category);
	}

	@Transactional
	public CategoryResponse disable(UUID id) {
		Category category = find(id);
		category.disable();
		return describe(category);
	}

	@Transactional
	public CategoryResponse enable(UUID id) {
		Category category = find(id);
		category.enable();
		return describe(category);
	}

	@Transactional
	public void delete(UUID id) {
		Category category = find(id);
		if (products.existsByCategory(category)) {
			throw new CategoryInUseException();
		}
		categories.delete(category);
	}

	private CategoryResponse describe(Category category) {
		return CategoryResponse.of(category, products.countByCategory(category));
	}

	private Category find(UUID id) {
		return categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

}
