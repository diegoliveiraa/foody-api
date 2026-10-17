package com.foody.api.auth.dto;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.validation.Password;
import com.foody.api.shared.util.EmailUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Email @Size(max = AuthConstants.EMAIL_MAX_LENGTH) String email,
		@NotNull @Password String password) {

	public LoginRequest {
		email = EmailUtils.normalize(email);
	}

}
