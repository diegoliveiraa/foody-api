package com.foody.api.shared.probe;

import java.util.List;
import java.util.Set;

public final class ProbeConstants {

	public static final String BASE_PATH = "/probe";

	public static final String SECRET_DETAIL = "connection string with credentials";

	public static final Set<String> SORTABLE_PROPERTIES = Set.of("name");

	public static final List<String> ITEMS = List.of("a", "b");

	public static final long TOTAL_ITEMS = 42;

	private ProbeConstants() {
	}

}
