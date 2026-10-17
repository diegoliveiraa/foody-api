package com.foody.api.shared.security;

public enum Module {

	STORE("store", "Loja"), DASHBOARD("dashboard", "Dashboard"), ORDERS("orders", "Pedidos"),
	CATALOG("catalog", "Catálogo"), ADMIN("admin", "Administração");

	private final String key;

	private final String label;

	Module(String key, String label) {
		this.key = key;
		this.label = label;
	}

	public String key() {
		return key;
	}

	public String label() {
		return label;
	}

}
