package com.foody.testsupport.openapi;

import java.nio.file.Path;
import java.util.List;

public final class OpenApiConstants {

	public static final String TITLE = "Foody API";

	public static final String VERSION = "v1";

	public static final String SERVER_URL = "/";

	public static final String DOCS_PATH = "/v3/api-docs";

	public static final List<String> PUBLIC_PATHS = List.of(DOCS_PATH + "/**", "/swagger-ui/**", "/swagger-ui.html");

	public static final String BEARER_SCHEME = "bearer";

	public static final String BEARER_FORMAT = "JWT";

	public static final Path CONTRACT_FILE = Path.of("docs", "openapi.json");

	public static final String WINDOWS_LINE_BREAK = "\r\n";

	public static final String LINE_BREAK = "\n";

	public static final String NULL_TYPE = "null";

	public static final String PAGE_PARAMETER = "page";

	public static final String SORT_PARAMETER = "sort";

	public static final String SORT_DESCRIPTION = "property,asc or property,desc; repeat it to sort by more than one property";

	private OpenApiConstants() {
	}

}
