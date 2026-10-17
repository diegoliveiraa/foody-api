package com.foody.api.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = resolve(request.getHeader(ObservabilityConstants.REQUEST_ID_HEADER));
		MDC.put(ObservabilityConstants.REQUEST_ID_KEY, requestId);
		response.setHeader(ObservabilityConstants.REQUEST_ID_HEADER, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(ObservabilityConstants.REQUEST_ID_KEY);
		}
	}

	private String resolve(String candidate) {
		if (candidate != null && ObservabilityConstants.SAFE_REQUEST_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}

}
