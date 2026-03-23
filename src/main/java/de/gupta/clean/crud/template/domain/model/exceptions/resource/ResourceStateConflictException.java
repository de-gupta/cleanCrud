package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class ResourceStateConflictException extends DomainException
{
	public static ResourceStateConflictException withMessage(final String message)
	{
		return new ResourceStateConflictException(message);
	}

	private ResourceStateConflictException(final String message)
	{
		super(message);
	}
}
