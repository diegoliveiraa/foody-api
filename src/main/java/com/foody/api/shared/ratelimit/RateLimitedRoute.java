package com.foody.api.shared.ratelimit;

import com.foody.api.shared.route.RouteConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public enum RateLimitedRoute {

	LOGIN(HttpMethod.POST, RouteConstants.LOGIN_PATH, RateLimitPolicy.LOGIN, RateLimitKey.IP),
	REGISTER(HttpMethod.POST, RouteConstants.REGISTER_PATH, RateLimitPolicy.REGISTER, RateLimitKey.IP),
	REFRESH(HttpMethod.POST, RouteConstants.REFRESH_PATH, RateLimitPolicy.REFRESH, RateLimitKey.IP),
	PLACE_ORDER(HttpMethod.POST, RouteConstants.MY_ORDERS_PATH, RateLimitPolicy.PLACE_ORDER, RateLimitKey.USER),
	REQUEST_CANCELLATION(HttpMethod.POST, RouteConstants.MY_ORDER_CANCELLATION_REQUEST_PATH,
			RateLimitPolicy.REQUEST_CANCELLATION, RateLimitKey.USER);

	private final RequestMatcher matcher;

	private final RateLimitPolicy policy;

	private final RateLimitKey key;

	RateLimitedRoute(HttpMethod method, String pathPattern, RateLimitPolicy policy, RateLimitKey key) {
		this.matcher = PathPatternRequestMatcher.withDefaults().matcher(method, pathPattern);
		this.policy = policy;
		this.key = key;
	}

	public static RateLimitTarget targetOf(HttpServletRequest request, RateLimitCaller caller) {
		return Arrays.stream(values())
			.map(route -> route.targetFor(request, caller))
			.flatMap(Optional::stream)
			.findFirst()
			.orElseGet(() -> RateLimitTarget.generalFor(caller));
	}

	private Optional<RateLimitTarget> targetFor(HttpServletRequest request, RateLimitCaller caller) {
		if (!matcher.matches(request)) {
			return Optional.empty();
		}
		return key.identify(caller).map(identifier -> new RateLimitTarget(policy, identifier));
	}

}
