package com.foody.api.auth.service;

import com.foody.api.user.event.UserDisabledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserDisabledListener {

	private final RefreshTokenService refreshTokens;

	public UserDisabledListener(RefreshTokenService refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	@EventListener
	public void revokeRefreshTokens(UserDisabledEvent event) {
		refreshTokens.revokeAllOf(event.userId());
	}

}
