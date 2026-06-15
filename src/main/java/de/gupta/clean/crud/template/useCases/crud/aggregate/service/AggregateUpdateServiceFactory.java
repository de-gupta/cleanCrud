package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateUpdateService;
import de.gupta.clean.crud.template.domain.service.aggregate.DefaultAggregateUpdateService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdateService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public enum AggregateUpdateServiceFactory
{
	;

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId> updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		return updateService(definition, DefaultAggregateUpdateService.create(definition, engine),
				_ -> List.of());
	}

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId> updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> aggregateUpdateService,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return new UpdateAggregateCrudService<>(definition, aggregateUpdateService, durableProcessStartRequests);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId> updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycle engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return updateService(definition, DefaultAggregateUpdateService.create(definition, engine),
				durableProcessStartRequests);
	}

	static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId> updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> aggregateUpdateService)
	{
		return updateService(definition, aggregateUpdateService, _ -> List.of());
	}

	private static final class UpdateAggregateCrudService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
			extends
			AbstractUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> implements
			UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	{
		private final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
				durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(context));
		}

		private UpdateAggregateCrudService(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateUpdateService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> aggregateUpdateService,
				final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
		{
			super(definition, aggregateUpdateService);
			this.durableProcessStartRequests = durableProcessStartRequests;
		}
	}
}