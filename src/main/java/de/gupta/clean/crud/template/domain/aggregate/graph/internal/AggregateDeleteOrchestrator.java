package de.gupta.clean.crud.template.domain.aggregate.graph.internal;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateBulkOperationMode;

import java.util.Collection;

public interface AggregateDeleteOrchestrator<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	PostCommitMutationContext<DomainId, DomainModel> deleteById(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final DomainId id);

	Collection<PostCommitMutationContext<DomainId, DomainModel>> deleteAllById(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final Collection<DomainId> ids,
			final AggregateBulkOperationMode mode);
}