package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.function.Function;

public interface AggregateDeleteService<DomainId, DomainModel>
{
	void deleteById(final DomainId id,
	                final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);

	void deleteAllById(final Collection<DomainId> ids, final AggregateBulkOperationMode mode,
	                   final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);
}