package com.foody.api.auth;

public final class AuthConstants {

	public static final String REFRESH_COOKIE_NAME = "foody_refresh";

	public static final String REFRESH_COOKIE_SAME_SITE = "Strict";

	public static final String CLEARED_COOKIE_VALUE = "";

	public static final int REFRESH_TOKEN_BYTES = 32;

	public static final int NAME_MIN_LENGTH = 2;

	public static final int NAME_MAX_LENGTH = 120;

	public static final int EMAIL_MAX_LENGTH = 254;

	public static final int PASSWORD_MIN_LENGTH = 8;

	public static final int PASSWORD_MAX_BYTES = 72;

	private AuthConstants() {
	}

}
