package com.foody.api.shared.observability;

import java.util.regex.Pattern;

public final class ObservabilityConstants {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	public static final String REQUEST_ID_KEY = "requestId";

	public static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	private ObservabilityConstants() {
	}

}
