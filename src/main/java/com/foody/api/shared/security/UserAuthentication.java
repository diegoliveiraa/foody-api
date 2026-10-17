package com.foody.api.shared.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class UserAuthentication extends AbstractAuthenticationToken {

	private final AuthenticatedUser user;

	private final Jwt token;

	public UserAuthentication(AuthenticatedUser user, Jwt token, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.user = user;
		this.token = token;
		setAuthenticated(true);
	}

	@Override
	public AuthenticatedUser getPrincipal() {
		return user;
	}

	@Override
	public Jwt getCredentials() {
		return token;
	}

	@Override
	public String getName() {
		return user.id().toString();
	}

}
