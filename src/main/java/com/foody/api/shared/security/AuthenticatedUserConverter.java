package com.foody.api.shared.security;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final ActiveUserLookup activeUsers;

	public AuthenticatedUserConverter(ActiveUserLookup activeUsers) {
		this.activeUsers = activeUsers;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		Role role = activeUsers.findActiveRole(userId).orElseThrow(AccountUnavailableException::new);
		return new UserAuthentication(new AuthenticatedUser(userId), jwt, RoleGrants.authoritiesOf(role));
	}

}
