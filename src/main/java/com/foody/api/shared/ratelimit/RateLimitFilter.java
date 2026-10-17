package com.foody.api.shared.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiter rateLimiter;

	private final HandlerExceptionResolver exceptionResolver;

	private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

	public RateLimitFilter(RateLimiter rateLimiter, HandlerExceptionResolver exceptionResolver) {
		this.rateLimiter = rateLimiter;
		this.exceptionResolver = exceptionResolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return RateLimitConstants.UNLIMITED_REQUESTS.matches(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		RateLimitTarget target = RateLimitedRoute.targetOf(request, callerOf(request));
		Optional<Duration> wait = rateLimiter.tryConsume(target);
		if (wait.isPresent()) {
			exceptionResolver.resolveException(request, response, null, new RateLimitExceededException(wait.get()));
			return;
		}
		chain.doFilter(request, response);
	}

	private RateLimitCaller callerOf(HttpServletRequest request) {
		String userName = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
			.filter(trustResolver::isAuthenticated)
			.map(Authentication::getName)
			.orElse(null);
		return new RateLimitCaller(userName, request.getRemoteAddr());
	}

}
