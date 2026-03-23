package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

import java.util.function.Supplier;

public final class NonUniqueResourceException extends DomainException
{
	public static NonUniqueResourceException withMessage(final String message)
	{
		return new NonUniqueResourceException(message);
	}

	public static Supplier<NonUniqueResourceException> forMessage(final String message)
	{
		return () -> withMessage(message);
	}

	private NonUniqueResourceException(final String message)
	{
		super(message);
	}
}