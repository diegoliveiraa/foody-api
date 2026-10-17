package com.foody.testsupport.openapi;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class RequiredByDefaultModelConverter implements ModelConverter {

	@Override
	public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
		if (!chain.hasNext()) {
			return null;
		}
		Schema<?> resolved = chain.next().resolve(type, context, chain);
		Class<?> rawClass = Json.mapper().constructType(type.getType()).getRawClass();
		if (resolved == null || !rawClass.isRecord()) {
			return resolved;
		}
		modelOf(resolved, rawClass, context).ifPresent(model -> applyTo(model, rawClass));
		return resolved;
	}

	private Optional<Schema<?>> modelOf(Schema<?> resolved, Class<?> rawClass, ModelConverterContext context) {
		if (resolved.get$ref() != null) {
			Schema<?> referenced = context.getDefinedModels()
				.get(resolved.get$ref().substring(Components.COMPONENTS_SCHEMAS_REF.length()));
			return Optional.ofNullable(referenced);
		}
		if (resolved.getProperties() != null) {
			return Optional.of(resolved);
		}
		Schema<?> defined = context.getDefinedModels().get(rawClass.getSimpleName());
		return Optional.ofNullable(defined);
	}

	private void applyTo(Schema<?> model, Class<?> record) {
		if (model.getProperties() == null) {
			return;
		}
		Arrays.stream(record.getRecordComponents())
			.filter(component -> model.getProperties().containsKey(component.getName()))
			.forEach(component -> apply(model, component));
	}

	private void apply(Schema<?> model, RecordComponent component) {
		String name = component.getName();
		if (!component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
			requireOnce(model, name);
			return;
		}
		Schema<?> property = model.getProperties().get(name);
		if (property.get$ref() != null) {
			model.getProperties().put(name, referenceOrNull(property));
			return;
		}
		allowNull(property);
	}

	private void requireOnce(Schema<?> model, String name) {
		if (model.getRequired() != null && model.getRequired().contains(name)) {
			return;
		}
		model.addRequiredItem(name);
	}

	private Schema<Object> referenceOrNull(Schema<?> property) {
		Schema<Object> reference = new Schema<>();
		reference.set$ref(property.get$ref());
		Schema<Object> nullType = new Schema<>();
		nullType.addType(OpenApiConstants.NULL_TYPE);
		Schema<Object> wrapper = new Schema<>();
		wrapper.setDescription(property.getDescription());
		wrapper.addOneOfItem(reference);
		wrapper.addOneOfItem(nullType);
		return wrapper;
	}

	private void allowNull(Schema<?> property) {
		Set<String> types = new LinkedHashSet<>(Optional.ofNullable(property.getTypes()).orElseGet(Set::of));
		Optional.ofNullable(property.getType()).ifPresent(types::add);
		if (types.isEmpty()) {
			return;
		}
		types.add(OpenApiConstants.NULL_TYPE);
		property.setTypes(types);
	}

}
