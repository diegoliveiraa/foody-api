package com.foody.api.shared.security;

import java.util.List;

public enum Screen {

	STORE_MENU(Module.STORE, "menu", "Cardápio", List.of(Action.PLACE_ORDER)),
	STORE_MY_ORDERS(Module.STORE, "my-orders", "Pedidos", List.of(Action.CANCEL, Action.REQUEST_CANCELLATION)),
	DASHBOARD_OVERVIEW(Module.DASHBOARD, "overview", "Visão geral", List.of()),
	ORDERS_ORDER_LIST(Module.ORDERS, "order-list", "Pedidos",
			List.of(Action.UPDATE_STATUS, Action.CANCEL, Action.DECIDE_CANCELLATION)),
	CATALOG_PRODUCTS(Module.CATALOG, "products", "Produtos", SecurityConstants.CATALOG_ACTIONS),
	CATALOG_CATEGORIES(Module.CATALOG, "categories", "Categorias", SecurityConstants.CATALOG_ACTIONS),
	ADMIN_USERS(Module.ADMIN, "users", "Usuários", List.of(Action.UPDATE, Action.DISABLE));

	private final Module module;

	private final String key;

	private final String label;

	private final List<Action> actions;

	Screen(Module module, String name, String label, List<Action> actions) {
		this.module = module;
		this.key = module.key() + SecurityConstants.SCREEN_KEY_SEPARATOR + name;
		this.label = label;
		this.actions = actions;
	}

	public Module module() {
		return module;
	}

	public String key() {
		return key;
	}

	public String label() {
		return label;
	}

	public List<Action> actions() {
		return actions;
	}

}
