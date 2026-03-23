package de.gupta.clean.crud.template.domain.model.exceptions;

public abstract class DomainException extends RuntimeException
{
	protected DomainException(final String message)
	{
		super(message);
	}
}