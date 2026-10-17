package com.foody.api.shared.ratelimit;

import com.foody.api.shared.route.RouteConstants;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class RateLimitConstants {

	public static final String PROPERTIES_PREFIX = "foody.rate-limit";

	public static final RequestMatcher UNLIMITED_REQUESTS = PathPatternRequestMatcher.withDefaults()
		.matcher(HttpMethod.GET, RouteConstants.HEALTH_PATH_PATTERN);

	public static final String KEY_SEPARATOR = "|";

	public static final long TOKENS_PER_REQUEST = 1;

	private RateLimitConstants() {
	}

}
