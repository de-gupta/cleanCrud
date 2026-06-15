package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateSaveService;
import de.gupta.clean.crud.template.domain.service.aggregate.DefaultAggregateSaveService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SaveService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.function.Function;

public enum AggregateSaveServices
{
	;

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
	saveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return saveService(definition, DefaultAggregateSaveService.create(definition, engine));
	}

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
	saveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateSaveService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate> aggregateSaveService
	)
	{
		return saveService(definition, aggregateSaveService, _ -> java.util.List.of());
	}

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
	saveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateSaveService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate> aggregateSaveService,
			final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests
	)
	{
		return new SaveAggregateCrudService<>(definition, aggregateSaveService, durableProcessStartRequests);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return saveService(definition, DefaultAggregateSaveService.create(definition, engine),
				durableProcessStartRequests);
	}

	private static final class SaveAggregateCrudService<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
			extends AbstractSaveService<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
			implements SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
	{
		private final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
				Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> savedModels)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(savedModels));
		}

		private SaveAggregateCrudService(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateSaveService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate> aggregateSaveService,
				final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
						Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
		{
			super(definition, aggregateSaveService);
			this.durableProcessStartRequests = durableProcessStartRequests;
		}
	}

}