package de.gupta.clean.crud.template.domain.model.exceptions;

public final class InvalidRequestException extends RuntimeException
{
	public static InvalidRequestException withMessage(final String message)
	{
		return new InvalidRequestException(message);
	}

	private InvalidRequestException(final String message)
	{
		super(message);
	}
}