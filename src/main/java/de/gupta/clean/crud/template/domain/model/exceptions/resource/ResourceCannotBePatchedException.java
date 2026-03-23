package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class ResourceCannotBePatchedException extends DomainException
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
