package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface CrudWorkflow<Result>
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
