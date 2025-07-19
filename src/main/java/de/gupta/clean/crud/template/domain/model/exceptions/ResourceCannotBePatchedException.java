package de.gupta.clean.crud.template.domain.model.exceptions;

public final class ResourceCannotBePatchedException extends RuntimeException
{
	public static ResourceCannotBePatchedException withMessage(final String message)
	{
		return new ResourceCannotBePatchedException(message);
	}

	private ResourceCannotBePatchedException(final String message)
	{
		super(message);
	}
}