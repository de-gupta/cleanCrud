package de.gupta.clean.crud.template.domain.model.exceptions.operation;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class InvalidRequestException extends DomainException
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
