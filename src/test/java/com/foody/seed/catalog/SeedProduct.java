package com.foody.seed.catalog;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record SeedProduct(String name, String description, @Nullable BigDecimal prototypePrice) {

	public static SeedProduct of(String name, String description) {
		return new SeedProduct(name, description, null);
	}

	public static SeedProduct prototype(String name, String price, String description) {
		return new SeedProduct(name, description, new BigDecimal(price));
	}

	public boolean isPrototype() {
		return prototypePrice != null;
	}

}
