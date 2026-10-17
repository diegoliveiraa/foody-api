package com.foody.api.auth.dto;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.validation.Password;
import com.foody.api.shared.util.EmailUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record RegisterRequest(
		@NotBlank @Size(min = AuthConstants.NAME_MIN_LENGTH, max = AuthConstants.NAME_MAX_LENGTH) String name,
		@NotBlank @Email @Size(max = AuthConstants.EMAIL_MAX_LENGTH) String email, @NotNull @Password String password) {

	public RegisterRequest {
		name = StringUtils.trimWhitespace(name);
		email = EmailUtils.normalize(email);
	}

}
