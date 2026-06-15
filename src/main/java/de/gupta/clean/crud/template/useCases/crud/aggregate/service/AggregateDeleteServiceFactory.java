package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateDeleteService;
import de.gupta.clean.crud.template.domain.service.aggregate.DefaultAggregateDeleteService;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeleteService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public enum AggregateDeleteServiceFactory
{
	;

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> DeleteService<MasterDomainId> deleteService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		return deleteService(DefaultAggregateDeleteService.create(definition, engine),
				_ -> java.util.List.of());
	}

	private static <MasterDomainId, MasterDomainModel> DeleteService<MasterDomainId> deleteService(
			final AggregateDeleteService<MasterDomainId, MasterDomainModel> aggregateDeleteService,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return new DeleteAggregateCrudService<>(aggregateDeleteService, durableProcessStartRequests);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> DeleteService<MasterDomainId> deleteService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycle engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return deleteService(DefaultAggregateDeleteService.create(definition, engine),
				durableProcessStartRequests);
	}

	static <MasterDomainId, MasterDomainModel> DeleteService<MasterDomainId> deleteService(
			final AggregateDeleteService<MasterDomainId, MasterDomainModel> aggregateDeleteService)
	{
		return deleteService(aggregateDeleteService, _ -> List.of());
	}

	private static final class DeleteAggregateCrudService<MasterDomainId, MasterDomainModel>
			extends AbstractDeleteService<MasterDomainId, MasterDomainModel> implements DeleteService<MasterDomainId>
	{
		private final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
				durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(context));
		}

		private DeleteAggregateCrudService(
				final AggregateDeleteService<MasterDomainId, MasterDomainModel> aggregateDeleteService,
				final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
		{
			super(aggregateDeleteService);
			this.durableProcessStartRequests = durableProcessStartRequests;
		}
	}
}