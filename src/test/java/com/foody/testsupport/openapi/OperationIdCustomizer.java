package com.foody.testsupport.openapi;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

public class OperationIdCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		String controller = StringUtils.uncapitalize(handlerMethod.getBeanType().getSimpleName())
			.replaceFirst(OpenApiConstants.CONTROLLER_SUFFIX + "$", "");
		String method = StringUtils.capitalize(handlerMethod.getMethod().getName());
		return operation.operationId(controller + method);
	}

}
