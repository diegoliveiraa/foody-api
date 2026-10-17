package com.foody.api.auth.dto;

public record AuthSession(AccessToken accessToken, IssuedRefreshToken refreshToken) {
}
