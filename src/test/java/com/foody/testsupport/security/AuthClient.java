package com.foody.testsupport.security;

import com.foody.api.auth.AuthConstants;
import com.foody.api.shared.security.SecurityConstants;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@TestComponent
public class AuthClient {

	private static final String LOGIN_REQUEST = """
			{ "email": "%s", "password": "%s" }
			""";

	private static final String ACCESS_TOKEN_PATH = "$.accessToken";

	private final MockMvcTester mvc;

	public AuthClient(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	public MvcTestResult login(String email, String password) {
		return mvc.post()
			.uri(SecurityConstants.LOGIN_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content(LOGIN_REQUEST.formatted(email, password))
			.exchange();
	}

	public MvcTestResult refresh(String refreshToken) {
		return withRefreshCookie(mvc.post().uri(SecurityConstants.REFRESH_PATH), refreshToken).exchange();
	}

	public MvcTestResult logout(String refreshToken) {
		return withRefreshCookie(mvc.post().uri(SecurityConstants.LOGOUT_PATH), refreshToken).exchange();
	}

	public String refreshTokenOf(MvcTestResult result) {
		return result.getResponse().getCookie(AuthConstants.REFRESH_COOKIE_NAME).getValue();
	}

	public String accessTokenOf(MvcTestResult result) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), ACCESS_TOKEN_PATH);
		}
		catch (UnsupportedEncodingException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private MockMvcRequestBuilder withRefreshCookie(MockMvcRequestBuilder request, String refreshToken) {
		return Optional.ofNullable(refreshToken)
			.map(value -> request.cookie(new Cookie(AuthConstants.REFRESH_COOKIE_NAME, value)))
			.orElse(request);
	}

}
