package com.foody.api.shared.security;

import com.foody.api.shared.observability.ObservabilityConstants;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public final class SecurityConstants {

	public static final String AUTH_PATH = "/auth";

	public static final String REGISTER_PATH = AUTH_PATH + "/register";

	public static final String LOGIN_PATH = AUTH_PATH + "/login";

	public static final String REFRESH_PATH = AUTH_PATH + "/refresh";

	public static final String LOGOUT_PATH = AUTH_PATH + "/logout";

	public static final String HEALTH_PATH_PATTERN = "/actuator/health/**";

	public static final String JWT_PROPERTIES_PREFIX = "foody.security.jwt";

	public static final String JWT_PUBLIC_KEY_PROPERTY = JWT_PROPERTIES_PREFIX + ".public-key";

	public static final String JWT_PRIVATE_KEY_PROPERTY = JWT_PROPERTIES_PREFIX + ".private-key";

	public static final String DEFAULT_ISSUER = "foody-api";

	public static final String DEFAULT_ACCESS_TOKEN_TTL = "PT15M";

	public static final String DEFAULT_REFRESH_TOKEN_TTL = "P7D";

	public static final String DEFAULT_REFRESH_FAMILY_TTL = "P30D";

	public static final String WEB_PROPERTIES_PREFIX = "foody.web";

	public static final String CORS_PATH_PATTERN = "/**";

	public static final List<String> CORS_ALLOWED_METHODS = Stream
		.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS)
		.map(HttpMethod::name)
		.toList();

	public static final List<String> CORS_ALLOWED_HEADERS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
			ObservabilityConstants.REQUEST_ID_HEADER);

	public static final List<String> CORS_EXPOSED_HEADERS = List.of(HttpHeaders.RETRY_AFTER,
			ObservabilityConstants.REQUEST_ID_HEADER);

	public static final String SCREEN_KEY_SEPARATOR = ".";

	public static final String AUTHORITY_SEPARATOR = ":";

	public static final String AUTHENTICATED_EXPRESSION = "isAuthenticated()";

	public static final String SCREEN_EXPRESSION = "hasAuthority('{value}')";

	public static final String GRANT_EXPRESSION = "hasAuthority('{screen}" + AUTHORITY_SEPARATOR + "{action}')";

	public static final List<Action> CATALOG_ACTIONS = List.of(Action.CREATE, Action.UPDATE, Action.DISABLE,
			Action.DELETE);

	private SecurityConstants() {
	}

}
