package com.foody.api.shared.security;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = SecurityConstants.WEB_PROPERTIES_PREFIX)
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
