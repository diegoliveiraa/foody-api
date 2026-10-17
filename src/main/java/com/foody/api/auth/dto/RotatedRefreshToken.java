package com.foody.api.auth.dto;

import java.util.UUID;

public record RotatedRefreshToken(UUID userId, IssuedRefreshToken refreshToken) {
}
