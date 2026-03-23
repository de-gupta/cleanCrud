package de.gupta.clean.crud.template.domain.model.exceptions.security;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class AccessDeniedException extends DomainException
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
