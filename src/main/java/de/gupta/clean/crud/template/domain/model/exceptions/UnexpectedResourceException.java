package de.gupta.clean.crud.template.domain.model.exceptions;

public final class UnexpectedResourceException extends RuntimeException
{
	public static UnexpectedResourceException withMessage(final String message)
	{
		return new UnexpectedResourceException(message);
	}

	private UnexpectedResourceException(final String message)
	{
		super(message);
	}
}