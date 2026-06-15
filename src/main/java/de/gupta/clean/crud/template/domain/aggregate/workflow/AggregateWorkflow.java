package de.gupta.clean.crud.template.domain.aggregate.workflow;

import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface AggregateWorkflow<Result>
{
	Result inTransaction();

	default void afterTransaction(final Result result)
	{
	}

	default boolean readOnly()
	{
		return false;
	}

	default Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(final Result result)
	{
		return List.of();
	}
}