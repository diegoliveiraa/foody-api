package com.foody.api.catalog.service;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.ProductFilter;
import com.foody.api.catalog.dto.ProductRequest;
import com.foody.api.catalog.dto.ProductResponse;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.exception.ProductInUseException;
import com.foody.api.catalog.exception.ProductNotFoundException;
import com.foody.api.catalog.exception.UnknownCategoryException;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.shared.pagination.PageRequests;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final List<ProductReferenceLookup> references;

	public ProductService(ProductRepository products, CategoryRepository categories,
			List<ProductReferenceLookup> references) {
		this.products = products;
		this.categories = categories;
		this.references = references;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> list(ProductFilter filter, Pageable pageable) {
		Pageable sorted = PageRequests.sortedOrDefault(pageable, CatalogConstants.PRODUCT_DEFAULT_SORT);
		return products.findAllMatching(filter, sorted).map(ProductResponse::of);
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Category category = categoryOf(request.categoryId());
		Product product = products
			.saveAndFlush(new Product(category, request.name(), request.description(), request.price()));
		return ProductResponse.of(product);
	}

	@Transactional
	public ProductResponse update(UUID id, ProductRequest request) {
		Product product = find(id);
		Category category = categoryOf(request.categoryId());
		product.update(category, request.name(), request.description(), request.price());
		return ProductResponse.of(product);
	}

	@Transactional
	public ProductResponse disable(UUID id) {
		Product product = find(id);
		product.disable();
		return ProductResponse.of(product);
	}

	@Transactional
	public ProductResponse enable(UUID id) {
		Product product = find(id);
		product.enable();
		return ProductResponse.of(product);
	}

	@Transactional
	public void delete(UUID id) {
		Product product = find(id);
		if (references.stream().anyMatch(lookup -> lookup.isReferenced(id))) {
			throw new ProductInUseException();
		}
		products.delete(product);
	}

	private Product find(UUID id) {
		return products.findWithCategoryById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	private Category categoryOf(UUID categoryId) {
		return categories.findById(categoryId).orElseThrow(() -> new UnknownCategoryException(categoryId));
	}

}
