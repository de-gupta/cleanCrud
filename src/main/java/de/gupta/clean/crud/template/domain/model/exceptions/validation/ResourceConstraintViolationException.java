package de.gupta.clean.crud.template.domain.model.exceptions.validation;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

import java.util.function.Supplier;

public final class ResourceConstraintViolationException extends DomainException
{
	public static Supplier<RuntimeException> forMessage(final String message)
	{
		return () -> withMessage(message);
	}

	public static RuntimeException withMessage(final String message)
	{
		return new ResourceConstraintViolationException(message);
	}

	private ResourceConstraintViolationException(final String message)
	{
		super(message);
	}
}