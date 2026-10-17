package com.foody.seed.user;

import com.foody.seed.SeedConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = SeedConstants.PROPERTIES_PREFIX)
public record IdentitySeedProperties(@Valid @NotNull Admin admin, @Valid @NotNull Account staff,
		@Valid @NotNull Account customer, @NotBlank String defaultPassword) {

	public record Admin(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record Account(@NotBlank @Email String email) {
	}

}
