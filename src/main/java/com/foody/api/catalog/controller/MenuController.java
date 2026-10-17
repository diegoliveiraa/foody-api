package com.foody.api.catalog.controller;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CartQuote;
import com.foody.api.catalog.dto.MenuResponse;
import com.foody.api.catalog.dto.QuoteRequest;
import com.foody.api.catalog.service.CartPricingService;
import com.foody.api.catalog.service.MenuService;
import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CatalogConstants.MENU_PATH)
public class MenuController {

	private final MenuService menu;

	private final CartPricingService cartPricing;

	public MenuController(MenuService menu, CartPricingService cartPricing) {
		this.menu = menu;
		this.cartPricing = cartPricing;
	}

	@GetMapping
	@RequiresScreen(Screen.STORE_MENU)
	public MenuResponse get() {
		return menu.menu();
	}

	@PostMapping(CatalogConstants.QUOTE_PATH)
	@RequiresGrant(screen = Screen.STORE_MENU, action = Action.PLACE_ORDER)
	public CartQuote quote(@Valid @RequestBody QuoteRequest request) {
		return cartPricing.quote(request.items());
	}

}
