package com.foody.testsupport.openapi;

import com.foody.api.shared.pagination.PaginationConstants;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.HandlerMethod;

public class PaginationParametersCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		Set<String> pageableNames = Arrays.stream(handlerMethod.getMethod().getParameters())
			.filter(parameter -> Pageable.class.isAssignableFrom(parameter.getType()))
			.map(Parameter::getName)
			.collect(Collectors.toSet());
		if (pageableNames.isEmpty()) {
			return operation;
		}
		operation.getParameters().removeIf(parameter -> pageableNames.contains(parameter.getName()));
		return operation.addParametersItem(page()).addParametersItem(pageSize()).addParametersItem(sort());
	}

	private QueryParameter page() {
		IntegerSchema schema = new IntegerSchema();
		schema.minimum(BigDecimal.valueOf(PaginationConstants.FIRST_PAGE_NUMBER));
		schema._default(PaginationConstants.FIRST_PAGE_NUMBER);
		return queryParameter(OpenApiConstants.PAGE_PARAMETER, schema);
	}

	private QueryParameter pageSize() {
		List<Number> allowedSizes = List.copyOf(PaginationConstants.ALLOWED_PAGE_SIZES);
		IntegerSchema schema = new IntegerSchema();
		schema._enum(allowedSizes);
		schema._default(PaginationConstants.DEFAULT_PAGE_SIZE);
		return queryParameter(PaginationConstants.PAGE_SIZE_PARAMETER, schema);
	}

	private QueryParameter sort() {
		QueryParameter parameter = queryParameter(OpenApiConstants.SORT_PARAMETER,
				new ArraySchema().items(new StringSchema()));
		parameter.description(OpenApiConstants.SORT_DESCRIPTION);
		return parameter;
	}

	private QueryParameter queryParameter(String name, Schema<?> schema) {
		QueryParameter parameter = new QueryParameter();
		parameter.name(name).required(false).schema(schema);
		return parameter;
	}

}
