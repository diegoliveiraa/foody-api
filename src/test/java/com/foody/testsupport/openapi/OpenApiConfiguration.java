package com.foody.testsupport.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	@Bean
	OpenAPI foodyOpenApi() {
		return new OpenAPI().info(new Info().title(OpenApiConstants.TITLE).version(OpenApiConstants.VERSION))
			.servers(List.of(new Server().url(OpenApiConstants.SERVER_URL)));
	}

}
