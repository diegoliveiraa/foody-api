package com.foody.seed.catalog;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogSeedConstants {

	public static final int PRODUCT_BASE_COUNT = 50;

	public static final int DISABLED_PRODUCT_PERCENT = 10;

	public static final int DISPLAY_ORDER_STEP = 10;

	public static final String NAME_SEPARATOR = " ";

	public static final List<String> NAME_VARIANTS = List.of("especial", "premium", "do chef", "tradicional", "gourmet",
			"artesanal", "da semana", "grande", "light", "mini");

	public static final BigDecimal PRICE_CENTS = new BigDecimal("0.90");

	public static final String PRODUCT_NAME_JOINER = " com ";

	public static final String DESCRIPTION_PREFIX = "Preparado com ";

	public static final String DESCRIPTION_JOINER = " e ";

	public static final int FACTORY_MIN_PRICE_CENTS = 500;

	public static final int FACTORY_MAX_PRICE_CENTS = 9990;

	private CatalogSeedConstants() {
	}

}
