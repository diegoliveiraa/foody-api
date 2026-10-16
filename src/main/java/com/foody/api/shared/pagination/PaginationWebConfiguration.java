package com.foody.api.shared.pagination;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class PaginationWebConfiguration implements WebMvcConfigurer {

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
		return resolver -> {
			resolver.setOneIndexedParameters(true);
			resolver.setSizeParameterName(PaginationConstants.PAGE_SIZE_PARAMETER);
			resolver.setMaxPageSize(PaginationConstants.MAX_PAGE_SIZE);
			resolver.setFallbackPageable(PageRequest.ofSize(PaginationConstants.DEFAULT_PAGE_SIZE));
		};
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new PageSizeInterceptor());
	}

}
