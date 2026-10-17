package com.foody.testsupport.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	@Bean
	OpenAPI foodyOpenApi() {
		SecurityScheme bearer = new SecurityScheme().type(SecurityScheme.Type.HTTP)
			.scheme(OpenApiConstants.BEARER_SCHEME)
			.bearerFormat(OpenApiConstants.BEARER_FORMAT);
		return new OpenAPI().info(new Info().title(OpenApiConstants.TITLE).version(OpenApiConstants.VERSION))
			.servers(List.of(new Server().url(OpenApiConstants.SERVER_URL)))
			.components(new Components().addSecuritySchemes(OpenApiConstants.BEARER_SCHEME, bearer));
	}

	@Bean
	RequiredByDefaultModelConverter requiredByDefaultModelConverter() {
		return new RequiredByDefaultModelConverter();
	}

	@Bean
	PaginationParametersCustomizer paginationParametersCustomizer() {
		return new PaginationParametersCustomizer();
	}

	@Bean
	OperationCustomizer bearerOnProtectedOperations() {
		return (operation, handlerMethod) -> {
			if (!AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PreAuthorize.class)) {
				return operation;
			}
			return operation.addSecurityItem(new SecurityRequirement().addList(OpenApiConstants.BEARER_SCHEME));
		};
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain openApiSecurityFilterChain(HttpSecurity http) {
		return http.securityMatcher(OpenApiConstants.PUBLIC_PATHS.toArray(String[]::new))
			.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
			.csrf(AbstractHttpConfigurer::disable)
			.build();
	}

}
