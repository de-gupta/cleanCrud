package de.gupta.clean.crud.template.infrastructure.web;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.ComparisonNotAllowedException;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.*;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.FieldValidationFailedException;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.RequiredFieldNotSetException;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.ValidationFailedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

public class AbstractSpringRestControllerAdvice
{
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<String> handleAccessDeniedException(final AccessDeniedException e)
	{
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<String> handleResourceNotFoundException(final ResourceNotFoundException e)
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	@ExceptionHandler(DependentResourceNotFoundException.class)
	public ResponseEntity<String> handleDependentResourceNotFoundException(final DependentResourceNotFoundException e)
	{
		return badRequest(e);
	}

	private static ResponseEntity<String> badRequest(final RuntimeException e)
	{
		return ResponseEntity.badRequest().body(e.getMessage());
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<String> handleResourceAlreadyExistsException(final ResourceAlreadyExistsException e)
	{
		return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<String> handleInvalidRequestException(final InvalidRequestException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e)
	{
		final String errorMessage =
				e.getBindingResult().getFieldErrors().stream().map(this::getErrorMessageForFieldError)
				 .collect(Collectors.joining(", "));
		return badRequest(errorMessage);
	}

	private String getErrorMessageForFieldError(FieldError fieldError)
	{
		return fieldError.getField() + ": " + fieldError.getDefaultMessage();
	}

	private static ResponseEntity<String> badRequest(final String message)
	{
		return ResponseEntity.badRequest().body(message);
	}

	@ExceptionHandler(ValidationFailedException.class)
	public ResponseEntity<String> handleValidationFailedException(final ValidationFailedException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(FieldValidationFailedException.class)
	public ResponseEntity<String> handleFieldValidationFailedException(final FieldValidationFailedException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(RequiredFieldNotSetException.class)
	public ResponseEntity<String> handleRequiredFieldNotSetException(final RequiredFieldNotSetException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<String> handleConstraintViolationException(final ConstraintViolationException e)
	{
		final String errorMessage =
				e.getConstraintViolations().stream().map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
				 .collect(Collectors.joining(", "));
		return badRequest(errorMessage);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<String> handleHandlerMethodValidationException(final HandlerMethodValidationException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<String> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(ResourceCannotBeDeletedException.class)
	public ResponseEntity<String> handleResourceCannotBeDeletedException(final ResourceCannotBeDeletedException e)
	{
		return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
	}

	@ExceptionHandler(ResourceCannotBePatchedException.class)
	public ResponseEntity<String> handleResourceCannotBePatchedException(final ResourceCannotBePatchedException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(ComparisonNotAllowedException.class)
	public ResponseEntity<String> handleComparisonNotAllowedException(final ComparisonNotAllowedException e)
	{
		return badRequest(e);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(final Exception e)
	{
		return ResponseEntity.internalServerError().body(e.getMessage());
	}
}