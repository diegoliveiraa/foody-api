package com.foody.api.auth.service;

import com.foody.api.auth.dto.AuthSession;
import com.foody.api.auth.dto.LoginRequest;
import com.foody.api.auth.dto.RegisterRequest;
import com.foody.api.auth.dto.RotatedRefreshToken;
import com.foody.api.auth.exception.EmailAlreadyRegisteredException;
import com.foody.api.auth.exception.InvalidCredentialsException;
import com.foody.api.user.entity.User;
import com.foody.api.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserService userService;

	private final PasswordEncoder passwordEncoder;

	private final AccessTokenService accessTokens;

	private final RefreshTokenService refreshTokens;

	private final String unknownUserPasswordHash;

	public AuthService(UserService userService, PasswordEncoder passwordEncoder, AccessTokenService accessTokens,
			RefreshTokenService refreshTokens) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.unknownUserPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional
	public AuthSession register(RegisterRequest request) {
		if (userService.existsByEmail(request.email())) {
			throw new EmailAlreadyRegisteredException();
		}
		String passwordHash = passwordEncoder.encode(request.password());
		User customer = userService.createCustomer(request.name(), request.email(), passwordHash);
		return startSession(customer);
	}

	@Transactional
	public AuthSession login(LoginRequest request) {
		Optional<User> candidate = userService.findByEmail(request.email());
		String passwordHash = candidate.map(User::getPasswordHash).orElse(unknownUserPasswordHash);
		boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
		User user = candidate.filter(found -> passwordMatches)
			.filter(User::isActive)
			.orElseThrow(InvalidCredentialsException::new);
		return startSession(user);
	}

	public AuthSession refresh(String refreshToken) {
		RotatedRefreshToken rotated = refreshTokens.rotate(refreshToken);
		return new AuthSession(accessTokens.issue(rotated.userId()), rotated.refreshToken());
	}

	public void logout(String refreshToken) {
		refreshTokens.revokeFamilyOf(refreshToken);
	}

	private AuthSession startSession(User user) {
		return new AuthSession(accessTokens.issue(user.getId()), refreshTokens.startFamily(user));
	}

}
