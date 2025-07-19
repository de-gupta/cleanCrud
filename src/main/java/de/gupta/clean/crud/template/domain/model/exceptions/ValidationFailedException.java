package de.gupta.clean.crud.template.domain.model.exceptions;

public final class ValidationFailedException extends RuntimeException
{
	public static ValidationFailedException withMessage(final String message)
	{
		return new ValidationFailedException(message);
	}

	private ValidationFailedException(final String message)
	{
		super(message);
	}
}