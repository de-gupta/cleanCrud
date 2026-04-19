package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeleteService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdateService;

public final class AggregateCrudServices
{
	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return new SaveAggregateCrudService<>(definition, engine);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return new FetchAggregateCrudService<>(definition, engine);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return new UpdateAggregateCrudService<>(definition, engine);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return new DeleteAggregateCrudService<>(definition, engine);
	}

	private AggregateCrudServices()
	{
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
	{
		private SaveAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class FetchAggregateCrudService<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
			extends AbstractFetchService<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	{
		private FetchAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class UpdateAggregateCrudService<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
			extends AbstractUpdateService<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	{
		private UpdateAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class DeleteAggregateCrudService<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
			extends AbstractDeleteService<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	{
		private DeleteAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}
}
