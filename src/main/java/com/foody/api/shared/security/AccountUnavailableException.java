package com.foody.api.shared.security;

import com.foody.api.shared.message.Messages;
import org.springframework.security.authentication.AccountStatusException;

public class AccountUnavailableException extends AccountStatusException {

	public AccountUnavailableException() {
		super(Messages.UNAUTHENTICATED.text());
	}

}
