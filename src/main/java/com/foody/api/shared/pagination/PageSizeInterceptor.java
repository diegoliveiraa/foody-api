package com.foody.api.shared.pagination;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class PageSizeInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String requested = request.getParameter(PaginationConstants.PAGE_SIZE_PARAMETER);
		if (requested == null || PaginationConstants.ALLOWED_PAGE_SIZE_VALUES.contains(requested)) {
			return true;
		}
		throw new InvalidPageSizeException();
	}

}
