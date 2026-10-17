package com.foody.api.auth.dto;

import java.time.Duration;

public record IssuedRefreshToken(String value, Duration lifetime) {
}
