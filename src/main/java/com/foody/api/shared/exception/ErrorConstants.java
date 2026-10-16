package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class ErrorConstants {

	public static final String PROPERTY_CODE = "code";

	public static final String PROPERTY_ERRORS = "errors";

	public static final String DETAIL_SEPARATOR = ": ";

	public static final String UNEXPECTED_ERROR_LOG = "Unhandled exception on {} {}";

	public static final String DATA_CONFLICT_LOG = "Data integrity violation";

	public static final Map<Integer, Messages> MESSAGES_BY_STATUS = Map.of(HttpStatus.BAD_REQUEST.value(),
			Messages.INVALID_PARAMETER, HttpStatus.NOT_FOUND.value(), Messages.RESOURCE_NOT_FOUND,
			HttpStatus.METHOD_NOT_ALLOWED.value(), Messages.METHOD_NOT_ALLOWED,
			HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), Messages.UNSUPPORTED_MEDIA_TYPE, HttpStatus.CONFLICT.value(),
			Messages.DATA_CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR.value(), Messages.INTERNAL_ERROR);

	private ErrorConstants() {
	}

}
