package com.foody.testsupport.order;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class OrderRequests {

	private static final String ORDER = """
			{ "items": [%s], "deliveryAddress": %s }
			""";

	private static final String ITEM = """
			{ "productId": "%s", "quantity": %d }
			""";

	private static final String ADDRESS = """
			{ "zipCode": %s, "street": %s, "number": %s, "complement": %s, "neighborhood": %s, "city": %s, "state": %s }
			""";

	private static final String ITEM_SEPARATOR = ",";

	private static final String QUOTE = "\"";

	private static final String NULL = "null";

	public static final String VALID_ADDRESS = address("01310-100", "Avenida Paulista", "1000", "Apto 42", "Bela Vista",
			"São Paulo", "SP");

	private OrderRequests() {
	}

	public static String order(String address, String... items) {
		return ORDER.formatted(String.join(ITEM_SEPARATOR, items), address);
	}

	public static String item(UUID productId, int quantity) {
		return ITEM.formatted(productId, quantity);
	}

	public static String address(@Nullable String zipCode, @Nullable String street, @Nullable String number,
			@Nullable String complement, @Nullable String neighborhood, @Nullable String city, @Nullable String state) {
		return ADDRESS.formatted(text(zipCode), text(street), text(number), text(complement), text(neighborhood),
				text(city), text(state));
	}

	private static String text(@Nullable String value) {
		if (value == null) {
			return NULL;
		}
		return QUOTE + value + QUOTE;
	}

}
