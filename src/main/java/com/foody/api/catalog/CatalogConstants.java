package com.foody.api.catalog;

import com.foody.api.shared.persistence.PersistenceConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.data.domain.Sort;

public final class CatalogConstants {

	public static final String CATEGORIES_PATH = "/catalog/categories";

	public static final String PRODUCTS_PATH = "/catalog/products";

	public static final String MENU_PATH = "/menu";

	public static final String QUOTE_PATH = "/quote";

	public static final int CATEGORY_NAME_MIN_LENGTH = 2;

	public static final int CATEGORY_NAME_MAX_LENGTH = 60;

	public static final int DISPLAY_ORDER_MIN = 0;

	public static final int DISPLAY_ORDER_MAX = 999;

	public static final int PRODUCT_NAME_MIN_LENGTH = 2;

	public static final int PRODUCT_NAME_MAX_LENGTH = 80;

	public static final int DESCRIPTION_MAX_LENGTH = 300;

	public static final String PRICE_EXCLUSIVE_MIN = "0";

	public static final String PRICE_MAX = "9999.99";

	public static final int PRICE_INTEGER_DIGITS = 4;

	public static final int PRICE_SCALE = 2;

	public static final RoundingMode PRICE_ROUNDING = RoundingMode.HALF_EVEN;

	public static final BigDecimal ZERO_PRICE = BigDecimal.ZERO.setScale(PRICE_SCALE);

	public static final long NO_PRODUCTS = 0;

	public static final int CART_MIN_ITEMS = 1;

	public static final int CART_MAX_ITEMS = 50;

	public static final int QUANTITY_MIN = 1;

	public static final int QUANTITY_MAX = 20;

	public static final String NAME_ATTRIBUTE = "name";

	public static final String PRICE_ATTRIBUTE = "price";

	public static final String CATEGORY_ATTRIBUTE = "category";

	public static final String DISPLAY_ORDER_ATTRIBUTE = "displayOrder";

	public static final Set<String> PRODUCT_SORTABLE_PROPERTIES = Set.of(NAME_ATTRIBUTE, PRICE_ATTRIBUTE,
			PersistenceConstants.CREATED_AT_ATTRIBUTE);

	public static final Sort PRODUCT_DEFAULT_SORT = Sort.by(NAME_ATTRIBUTE);

	public static final Sort CATEGORY_SORT = Sort.by(DISPLAY_ORDER_ATTRIBUTE, NAME_ATTRIBUTE);

	private CatalogConstants() {
	}

}
