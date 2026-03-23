package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class UnexpectedResourceException extends DomainException
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
