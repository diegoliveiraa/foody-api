package com.foody.testsupport.openapi;

import java.nio.file.Path;

public final class OpenApiConstants {

	public static final String TITLE = "Foody API";
	public static final String VERSION = "v1";
	public static final String SERVER_URL = "/";
	public static final String DOCS_PATH = "/v3/api-docs";
	public static final Path CONTRACT_FILE = Path.of("docs", "openapi.json");
	public static final String WINDOWS_LINE_BREAK = "\r\n";
	public static final String LINE_BREAK = "\n";

	private OpenApiConstants() {
	}

}
