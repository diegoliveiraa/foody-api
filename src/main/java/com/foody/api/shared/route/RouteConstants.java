package com.foody.api.shared.route;

public final class RouteConstants {

	public static final String ID_PATH = "/{id}";

	public static final String DISABLE_PATH = ID_PATH + "/disable";

	public static final String ENABLE_PATH = ID_PATH + "/enable";

	public static final String AUTH_PATH = "/auth";

	public static final String REGISTER_PATH = AUTH_PATH + "/register";

	public static final String LOGIN_PATH = AUTH_PATH + "/login";

	public static final String REFRESH_PATH = AUTH_PATH + "/refresh";

	public static final String LOGOUT_PATH = AUTH_PATH + "/logout";

	public static final String HEALTH_PATH_PATTERN = "/actuator/health/**";

	public static final String MY_ORDERS_PATH = "/my-orders";

	public static final String MY_ORDER_CANCELLATION_REQUEST_PATH = MY_ORDERS_PATH + ID_PATH + "/cancellation-request";

	private RouteConstants() {
	}

}
