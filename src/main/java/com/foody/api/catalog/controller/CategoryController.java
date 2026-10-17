package com.foody.api.catalog.controller;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CategoryRequest;
import com.foody.api.catalog.dto.CategoryResponse;
import com.foody.api.catalog.service.CategoryService;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CatalogConstants.CATEGORIES_PATH)
public class CategoryController {

	private final CategoryService categories;

	public CategoryController(CategoryService categories) {
		this.categories = categories;
	}

	@GetMapping
	@RequiresScreen(Screen.CATALOG_CATEGORIES)
	public List<CategoryResponse> list() {
		return categories.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresGrant(screen = Screen.CATALOG_CATEGORIES, action = Action.CREATE)
	public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
		return categories.create(request);
	}

	@PutMapping(RouteConstants.ID_PATH)
	@RequiresGrant(screen = Screen.CATALOG_CATEGORIES, action = Action.UPDATE)
	public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
		return categories.update(id, request);
	}

	@PostMapping(RouteConstants.DISABLE_PATH)
	@RequiresGrant(screen = Screen.CATALOG_CATEGORIES, action = Action.DISABLE)
	public CategoryResponse disable(@PathVariable UUID id) {
		return categories.disable(id);
	}

	@PostMapping(RouteConstants.ENABLE_PATH)
	@RequiresGrant(screen = Screen.CATALOG_CATEGORIES, action = Action.DISABLE)
	public CategoryResponse enable(@PathVariable UUID id) {
		return categories.enable(id);
	}

	@DeleteMapping(RouteConstants.ID_PATH)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresGrant(screen = Screen.CATALOG_CATEGORIES, action = Action.DELETE)
	public void delete(@PathVariable UUID id) {
		categories.delete(id);
	}

}
