package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateBulkOperationMode;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateUpdateService;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;

public abstract class AbstractUpdateService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse,
		MasterDomainId>
{
	private final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch> aggregateUpdateService;

	@Override
	public void putAtId(final MasterDomainId id, final MasterDomainModelCreate model)
	{
		aggregateUpdateService.putAtId(id, model, this::durableProcessStartRequests);
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> updateById(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		return identifiedModel(aggregateUpdateService.updateById(id, updatePatch, this::durableProcessStartRequests));
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		return aggregateUpdateService.updateAllById(models, aggregateBulkMode(mode), this::durableProcessStartRequests)
		                             .stream()
		                             .map(this::identifiedModel)
		                             .toList();
	}

	private AggregateBulkOperationMode aggregateBulkMode(final BulkOperationMode mode)
	{
		return switch (mode)
		{
			case ALL_OR_NOTHING -> AggregateBulkOperationMode.ALL_OR_NOTHING;
			case BEST_EFFORT -> AggregateBulkOperationMode.BEST_EFFORT;
		};
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), definition.responseBuilder().toResponse(domainModel.model()));
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> results)
	{
		return results.stream()
		              .map(this::durableProcessStartRequests)
		              .flatMap(Collection::stream)
		              .toList();
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
	{
		return List.of();
	}

	protected AbstractUpdateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch> aggregateUpdateService)
	{
		this.definition = definition;
		this.aggregateUpdateService = aggregateUpdateService;
	}
}