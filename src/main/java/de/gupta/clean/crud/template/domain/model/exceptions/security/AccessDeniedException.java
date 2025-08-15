package de.gupta.clean.crud.template.domain.model.exceptions.security;

public final class AccessDeniedException extends RuntimeException
{
	public static AccessDeniedException withMessage(final String message)
	{
		return new AccessDeniedException(message);
	}

	private AccessDeniedException(final String message)
	{
		super(message);
	}
}