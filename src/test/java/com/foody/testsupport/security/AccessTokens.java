package com.foody.testsupport.security;

import com.foody.api.auth.service.AccessTokenService;
import com.foody.api.shared.exception.ErrorConstants;
import com.foody.api.user.entity.User;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class AccessTokens {

	private static final String SCHEME_SEPARATOR = " ";

	private final AccessTokenService accessTokenService;

	public AccessTokens(AccessTokenService accessTokenService) {
		this.accessTokenService = accessTokenService;
	}

	public String bearer(User user) {
		return bearer(accessTokenService.issue(user.getId()).value());
	}

	public String bearer(String accessToken) {
		return ErrorConstants.BEARER_CHALLENGE + SCHEME_SEPARATOR + accessToken;
	}

}
