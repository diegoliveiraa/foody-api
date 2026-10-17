package com.foody.api.auth.dto;

public record AuthResponse(String accessToken, long expiresIn) {

	public static AuthResponse of(AccessToken accessToken) {
		return new AuthResponse(accessToken.value(), accessToken.expiresInSeconds());
	}

}
