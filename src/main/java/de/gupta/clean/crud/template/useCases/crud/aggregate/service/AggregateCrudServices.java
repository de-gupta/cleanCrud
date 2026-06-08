package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.*;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeleteService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdateService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.function.Function;

public final class AggregateCrudServices
{
	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return saveService(
				definition,
				engine,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.saveCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return saveService(
				definition,
				engine,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.saveCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateSaveCoordinator saveCoordinator)
	{
		return saveService(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
				saveCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId> saveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateSaveCoordinator saveCoordinator)
	{
		return new SaveAggregateCrudService<>(definition, engine, durableProcessStartRequests, definitionGuard,
				validationSupport, saveCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return fetchService(
				definition,
				engine,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.fetchCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateFetchCoordinator fetchCoordinator)
	{
		return new FetchAggregateCrudService<>(definition, engine, definitionGuard, fetchCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return updateService(
				definition,
				engine,
				_ -> java.util.List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.updateCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return updateService(
				definition,
				engine,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.updateCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateUpdateCoordinator updateCoordinator)
	{
		return updateService(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
				updateCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateUpdateCoordinator updateCoordinator)
	{
		return new UpdateAggregateCrudService<>(definition, engine, durableProcessStartRequests, definitionGuard,
				validationSupport, updateCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return deleteService(
				definition,
				engine,
				_ -> java.util.List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.deleteCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		return deleteService(
				definition,
				engine,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.deleteCoordinator());
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateDeleteCoordinator deleteCoordinator)
	{
		return deleteService(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
				deleteCoordinator);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateDeleteCoordinator deleteCoordinator)
	{
		return new DeleteAggregateCrudService<>(definition, engine, durableProcessStartRequests, definitionGuard,
				validationSupport, deleteCoordinator);
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
		private final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
				Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> savedModels)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(savedModels));
		}

		private SaveAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateSaveCoordinator saveCoordinator)
		{
			this(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport, saveCoordinator);
		}

		private SaveAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final Function<Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>,
						Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateSaveCoordinator saveCoordinator)
		{
			super(definition, engine, definitionGuard, validationSupport, saveCoordinator);
			this.durableProcessStartRequests = durableProcessStartRequests;
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
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateFetchCoordinator fetchCoordinator)
		{
			super(definition, engine, definitionGuard, fetchCoordinator);
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
		private final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
				Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(context));
		}

		private UpdateAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
						Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateUpdateCoordinator updateCoordinator)
		{
			super(definition, engine, definitionGuard, validationSupport, updateCoordinator);
			this.durableProcessStartRequests = durableProcessStartRequests;
		}

		private UpdateAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateUpdateCoordinator updateCoordinator)
		{
			this(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
					updateCoordinator);
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
		private final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
				Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests;

		@Override
		protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
				final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
		{
			return java.util.List.copyOf(durableProcessStartRequests.apply(context));
		}

		private DeleteAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final Function<PostCommitMutationContext<MasterDomainId, MasterDomainModel>,
						Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateDeleteCoordinator deleteCoordinator)
		{
			super(definition, engine, definitionGuard, validationSupport, deleteCoordinator);
			this.durableProcessStartRequests = durableProcessStartRequests;
		}

		private DeleteAggregateCrudService(
				final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateDeleteCoordinator deleteCoordinator)
		{
			this(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
					deleteCoordinator);
		}
	}
}