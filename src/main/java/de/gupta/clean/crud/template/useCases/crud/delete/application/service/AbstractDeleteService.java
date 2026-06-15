package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateBulkOperationMode;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateDeleteService;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;

public abstract class AbstractDeleteService<MasterDomainId, MasterDomainModel>
		implements DeleteService<MasterDomainId>
{
	private final AggregateDeleteService<MasterDomainId, MasterDomainModel> aggregateDeleteService;

	@Override
	public void deleteById(final MasterDomainId id)
	{
		aggregateDeleteService.deleteById(id, this::durableProcessStartRequests);
	}

	@Override
	public void deleteAllById(final Collection<MasterDomainId> ids, final BulkOperationMode mode)
	{
		aggregateDeleteService.deleteAllById(ids, aggregateBulkMode(mode), this::durableProcessStartRequests);
	}

	private AggregateBulkOperationMode aggregateBulkMode(final BulkOperationMode mode)
	{
		return switch (mode)
		{
			case ALL_OR_NOTHING -> AggregateBulkOperationMode.ALL_OR_NOTHING;
			case BEST_EFFORT -> AggregateBulkOperationMode.BEST_EFFORT;
		};
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> contexts)
	{
		return contexts.stream()
		               .map(this::durableProcessStartRequests)
		               .flatMap(Collection::stream)
		               .toList();
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
	{
		return List.of();
	}

	protected AbstractDeleteService(
			final AggregateDeleteService<MasterDomainId, MasterDomainModel> aggregateDeleteService)
	{
		this.aggregateDeleteService = aggregateDeleteService;
	}
}