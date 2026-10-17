package com.foody.api.order.util;

import com.foody.api.order.OrderConstants;
import java.util.Optional;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;

public final class OrderSearchUtils {

	private OrderSearchUtils() {
	}

	public static Optional<Long> orderNumber(@Nullable String search) {
		return Optional.ofNullable(search)
			.map(String::strip)
			.map(OrderConstants.ORDER_NUMBER_SEARCH_PATTERN::matcher)
			.filter(Matcher::matches)
			.map(matcher -> Long.valueOf(matcher.group(OrderConstants.ORDER_NUMBER_SEARCH_GROUP)));
	}

}
