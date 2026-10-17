package com.foody.api.shared.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfiguration {

	@Bean
	static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
		return new AnnotationTemplateExpressionDefaults();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, ActiveUserLookup activeUsers,
			CorsProperties corsProperties,
			@Qualifier(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME) HandlerExceptionResolver exceptionResolver) {
		AuthenticationEntryPoint entryPoint = (request, response, exception) -> exceptionResolver
			.resolveException(request, response, null, exception);
		AccessDeniedHandler accessDeniedHandler = (request, response, exception) -> exceptionResolver
			.resolveException(request, response, null, exception);
		http.csrf(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR)
				.permitAll()
				.requestMatchers(HttpMethod.POST, SecurityConstants.REGISTER_PATH, SecurityConstants.LOGIN_PATH,
						SecurityConstants.REFRESH_PATH, SecurityConstants.LOGOUT_PATH)
				.permitAll()
				.requestMatchers(HttpMethod.GET, SecurityConstants.HEALTH_PATH_PATTERN)
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.jwt(jwt -> jwt.decoder(jwtDecoder)
					.jwtAuthenticationConverter(new AuthenticatedUserConverter(activeUsers)))
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	private CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(properties.allowedOrigins());
		cors.setAllowedMethods(SecurityConstants.CORS_ALLOWED_METHODS);
		cors.setAllowedHeaders(SecurityConstants.CORS_ALLOWED_HEADERS);
		cors.setExposedHeaders(SecurityConstants.CORS_EXPOSED_HEADERS);
		cors.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration(SecurityConstants.CORS_PATH_PATTERN, cors);
		return source;
	}

}
