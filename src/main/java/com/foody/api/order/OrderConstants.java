package com.foody.api.order;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;

public final class OrderConstants {

	public static final String ORDERS_PATH = "/orders";

	public static final String PLACED_AT_ATTRIBUTE = "placedAt";

	public static final String TOTAL_ATTRIBUTE = "total";

	public static final String NUMBER_ATTRIBUTE = "number";

	public static final String STATUS_ATTRIBUTE = "status";

	public static final String CUSTOMER_ATTRIBUTE = "customer";

	public static final String ORDER_ATTRIBUTE = "order";

	public static final String ACTOR_ATTRIBUTE = "actor";

	public static final Set<String> SORTABLE_PROPERTIES = Set.of(PLACED_AT_ATTRIBUTE, TOTAL_ATTRIBUTE,
			NUMBER_ATTRIBUTE);

	public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, PLACED_AT_ATTRIBUTE);

	public static final int STREET_MIN_LENGTH = 2;

	public static final int STREET_MAX_LENGTH = 120;

	public static final int ADDRESS_NUMBER_MIN_LENGTH = 1;

	public static final int ADDRESS_NUMBER_MAX_LENGTH = 10;

	public static final int COMPLEMENT_MAX_LENGTH = 60;

	public static final int NEIGHBORHOOD_MIN_LENGTH = 2;

	public static final int NEIGHBORHOOD_MAX_LENGTH = 60;

	public static final int CITY_MIN_LENGTH = 2;

	public static final int CITY_MAX_LENGTH = 60;

	public static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^\\d{5}-?\\d{3}$");

	public static final String ZIP_CODE_SEPARATOR = "-";

	public static final int ZIP_CODE_PREFIX_LENGTH = 5;

	public static final Pattern ORDER_NUMBER_SEARCH_PATTERN = Pattern.compile("^#?(\\d{1,18})$");

	public static final int ORDER_NUMBER_SEARCH_GROUP = 1;

	private OrderConstants() {
	}

}
