package de.gupta.clean.crud.template.domain.aggregate.graph.internal;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.operation.AggregateBulkOperationMode;

import java.util.Collection;

public interface AggregateUpdateOrchestrator<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	PostCommitMutationContext<DomainId, DomainModel> putAtId(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final DomainId id,
			final DomainModelCreate model);

	UpdateResult<DomainId, DomainModel> updateById(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final DomainId id,
			final DomainModelUpdatePatch updatePatch);

	Collection<UpdateResult<DomainId, DomainModel>> updateAllById(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final Collection<IdentifiedModel<DomainId, DomainModelUpdatePatch>> models,
			final AggregateBulkOperationMode mode);

	record UpdateResult<DomainId, DomainModel>(IdentifiedModel<DomainId, DomainModel> updated,
	                                           PostCommitMutationContext<DomainId, DomainModel> context)
	{
	}
}