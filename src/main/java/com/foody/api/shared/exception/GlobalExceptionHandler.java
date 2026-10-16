package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomain(DomainException exception) {
		return problem(exception.status(), exception.messageKey(), exception.getMessage());
	}

	@ExceptionHandler(PropertyReferenceException.class)
	public ProblemDetail handleUnknownSortProperty(PropertyReferenceException exception) {
		return problem(HttpStatus.BAD_REQUEST, Messages.INVALID_SORT, Messages.INVALID_SORT.text());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
		return problem(HttpStatus.CONFLICT, Messages.CONCURRENT_UPDATE, Messages.CONCURRENT_UPDATE.text());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataConflict(DataIntegrityViolationException exception) {
		log.warn(ErrorConstants.DATA_CONFLICT_LOG, exception);
		return problem(HttpStatus.CONFLICT, Messages.DATA_CONFLICT, Messages.DATA_CONFLICT.text());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error(ErrorConstants.UNEXPECTED_ERROR_LOG, request.getMethod(), request.getRequestURI(), exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_ERROR, Messages.INTERNAL_ERROR.text());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();
		return handleExceptionInternal(exception, validationProblem(status, violations), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = exception.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new FieldViolation(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return handleExceptionInternal(exception, validationProblem(status, violations), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(status, Messages.MALFORMED_REQUEST, Messages.MALFORMED_REQUEST.text());
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(status, Messages.INVALID_PARAMETER, Messages.INVALID_PARAMETER.text());
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status,
			WebRequest request) {
		return super.createResponseEntity(withCatalogCode(body, status), headers, status, request);
	}

	private Object withCatalogCode(Object body, HttpStatusCode status) {
		if (!(body instanceof ProblemDetail problem) || hasCode(problem)) {
			return body;
		}
		Messages message = ErrorConstants.MESSAGES_BY_STATUS.getOrDefault(status.value(), Messages.REQUEST_FAILED);
		problem.setDetail(message.text());
		problem.setProperty(ErrorConstants.PROPERTY_CODE, message.name());
		return problem;
	}

	private boolean hasCode(ProblemDetail problem) {
		return problem.getProperties() != null && problem.getProperties().containsKey(ErrorConstants.PROPERTY_CODE);
	}

	private ProblemDetail validationProblem(HttpStatusCode status, List<FieldViolation> violations) {
		ProblemDetail problem = problem(status, Messages.VALIDATION_FAILED, Messages.VALIDATION_FAILED.text());
		problem.setProperty(ErrorConstants.PROPERTY_ERRORS, violations);
		return problem;
	}

	private ProblemDetail problem(HttpStatusCode status, Messages message, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty(ErrorConstants.PROPERTY_CODE, message.name());
		return problem;
	}

}
