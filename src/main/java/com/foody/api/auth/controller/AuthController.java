package com.foody.api.auth.controller;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.dto.AuthResponse;
import com.foody.api.auth.dto.AuthSession;
import com.foody.api.auth.dto.LoginRequest;
import com.foody.api.auth.dto.RegisterRequest;
import com.foody.api.auth.service.AuthService;
import com.foody.api.auth.util.RefreshCookies;
import com.foody.api.shared.security.SecurityConstants;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping(SecurityConstants.REGISTER_PATH)
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return withRefreshCookie(HttpStatus.CREATED, authService.register(request));
	}

	@PostMapping(SecurityConstants.LOGIN_PATH)
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return withRefreshCookie(HttpStatus.OK, authService.login(request));
	}

	@PostMapping(SecurityConstants.REFRESH_PATH)
	public ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = AuthConstants.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
		return withRefreshCookie(HttpStatus.OK, authService.refresh(refreshToken));
	}

	@PostMapping(SecurityConstants.LOGOUT_PATH)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> logout(
			@CookieValue(name = AuthConstants.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, RefreshCookies.cleared().toString()).build();
	}

	private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, AuthSession session) {
		return ResponseEntity.status(status)
			.header(HttpHeaders.SET_COOKIE, RefreshCookies.issued(session.refreshToken()).toString())
			.body(AuthResponse.of(session.accessToken()));
	}

}
