package com.foody.api.auth.util;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.dto.IssuedRefreshToken;
import com.foody.api.shared.route.RouteConstants;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;

public final class RefreshCookies {

	private RefreshCookies() {
	}

	public static ResponseCookie issued(IssuedRefreshToken token) {
		return cookie(token.value()).maxAge(token.lifetime()).build();
	}

	public static ResponseCookie cleared() {
		return cookie(AuthConstants.CLEARED_COOKIE_VALUE).maxAge(Duration.ZERO).build();
	}

	private static ResponseCookieBuilder cookie(String value) {
		return ResponseCookie.from(AuthConstants.REFRESH_COOKIE_NAME, value)
			.httpOnly(true)
			.secure(true)
			.sameSite(AuthConstants.REFRESH_COOKIE_SAME_SITE)
			.path(RouteConstants.AUTH_PATH);
	}

}
