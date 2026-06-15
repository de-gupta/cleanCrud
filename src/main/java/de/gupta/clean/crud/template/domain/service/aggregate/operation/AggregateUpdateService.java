package de.gupta.clean.crud.template.domain.service.aggregate.operation;

import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.function.Function;

public interface AggregateUpdateService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
{
	void putAtId(final DomainId id,
	             final DomainModelCreate model,
	             final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);

	IdentifiedModel<DomainId, DomainModel> updateById(final DomainId id,
	                                                  final DomainModelUpdatePatch updatePatch,
	                                                  final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);

	Collection<IdentifiedModel<DomainId, DomainModel>> updateAllById(
			final Collection<IdentifiedModel<DomainId, DomainModelUpdatePatch>> models,
			final AggregateBulkOperationMode mode,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);
}