package com.foody.api.auth.dto;

public record AccessToken(String value, long expiresInSeconds) {
}
