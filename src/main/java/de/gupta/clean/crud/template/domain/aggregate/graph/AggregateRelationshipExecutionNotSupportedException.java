package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class AggregateRelationshipExecutionNotSupportedException extends DomainException
{
	public static AggregateRelationshipExecutionNotSupportedException withMessage(final String message)
	{
		return new AggregateRelationshipExecutionNotSupportedException(message);
	}

	private AggregateRelationshipExecutionNotSupportedException(final String message)
	{
		super(message);
	}
}