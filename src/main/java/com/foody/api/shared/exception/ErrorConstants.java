package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import com.foody.api.shared.observability.ObservabilityConstants;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;

public final class ErrorConstants {

	public static final String PROPERTY_CODE = "code";

	public static final String PROPERTY_ERRORS = "errors";

	public static final String PROPERTY_REQUEST_ID = ObservabilityConstants.REQUEST_ID_KEY;

	public static final String DETAIL_SEPARATOR = ": ";

	public static final String BEARER_CHALLENGE = "Bearer";

	public static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);

	public static final long MIN_RETRY_AFTER_SECONDS = 1;

	public static final String UNEXPECTED_ERROR_LOG = "Unhandled exception on {} {}";

	public static final String DATA_CONFLICT_LOG = "Data integrity violation";

	public static final Map<Integer, Messages> MESSAGES_BY_STATUS = Map.ofEntries(
			Map.entry(HttpStatus.BAD_REQUEST.value(), Messages.INVALID_PARAMETER),
			Map.entry(HttpStatus.UNAUTHORIZED.value(), Messages.UNAUTHENTICATED),
			Map.entry(HttpStatus.FORBIDDEN.value(), Messages.FORBIDDEN),
			Map.entry(HttpStatus.NOT_FOUND.value(), Messages.RESOURCE_NOT_FOUND),
			Map.entry(HttpStatus.METHOD_NOT_ALLOWED.value(), Messages.METHOD_NOT_ALLOWED),
			Map.entry(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), Messages.UNSUPPORTED_MEDIA_TYPE),
			Map.entry(HttpStatus.CONFLICT.value(), Messages.DATA_CONFLICT),
			Map.entry(HttpStatus.TOO_MANY_REQUESTS.value(), Messages.RATE_LIMITED),
			Map.entry(HttpStatus.INTERNAL_SERVER_ERROR.value(), Messages.INTERNAL_ERROR));

	private ErrorConstants() {
	}

}
