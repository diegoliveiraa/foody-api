package com.foody.api.catalog.controller;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.ProductFilter;
import com.foody.api.catalog.dto.ProductRequest;
import com.foody.api.catalog.dto.ProductResponse;
import com.foody.api.catalog.service.ProductService;
import com.foody.api.shared.pagination.PageResponse;
import com.foody.api.shared.pagination.SortGuard;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CatalogConstants.PRODUCTS_PATH)
public class ProductController {

	private final ProductService products;

	public ProductController(ProductService products) {
		this.products = products;
	}

	@GetMapping
	@RequiresScreen(Screen.CATALOG_PRODUCTS)
	public PageResponse<ProductResponse> list(@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) ActivationStatus status, @RequestParam(required = false) String search,
			Pageable pageable) {
		Pageable allowed = SortGuard.requireAllowed(pageable, CatalogConstants.PRODUCT_SORTABLE_PROPERTIES);
		return PageResponse.of(products.list(new ProductFilter(categoryId, status, search), allowed));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresGrant(screen = Screen.CATALOG_PRODUCTS, action = Action.CREATE)
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return products.create(request);
	}

	@PutMapping(RouteConstants.ID_PATH)
	@RequiresGrant(screen = Screen.CATALOG_PRODUCTS, action = Action.UPDATE)
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
		return products.update(id, request);
	}

	@PostMapping(RouteConstants.DISABLE_PATH)
	@RequiresGrant(screen = Screen.CATALOG_PRODUCTS, action = Action.DISABLE)
	public ProductResponse disable(@PathVariable UUID id) {
		return products.disable(id);
	}

	@PostMapping(RouteConstants.ENABLE_PATH)
	@RequiresGrant(screen = Screen.CATALOG_PRODUCTS, action = Action.DISABLE)
	public ProductResponse enable(@PathVariable UUID id) {
		return products.enable(id);
	}

	@DeleteMapping(RouteConstants.ID_PATH)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresGrant(screen = Screen.CATALOG_PRODUCTS, action = Action.DELETE)
	public void delete(@PathVariable UUID id) {
		products.delete(id);
	}

}
