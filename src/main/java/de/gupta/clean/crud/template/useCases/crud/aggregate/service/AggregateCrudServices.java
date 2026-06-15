package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.execution.*;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateSaveService;
import de.gupta.clean.crud.template.domain.service.aggregate.DefaultAggregateSaveService;
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

public enum AggregateCrudServices
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

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		return fetchService(
				definition,
				engine,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.fetchCoordinator());
	}

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
	UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse, MasterDomainId>
	updateService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
	DeleteService<MasterDomainId> deleteService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	DeleteService<MasterDomainId> deleteService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateDeleteCoordinator deleteCoordinator)
	{
		return deleteService(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
				deleteCoordinator);
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
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateUpdateCoordinator updateCoordinator)
		{
			this(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
					updateCoordinator);
		}

		private UpdateAggregateCrudService(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final AggregateLifecycleEngine engine,
				final AggregateDefinitionGuard definitionGuard,
				final AggregateMutationValidationSupport validationSupport,
				final AggregateDeleteCoordinator deleteCoordinator)
		{
			this(definition, engine, _ -> java.util.List.of(), definitionGuard, validationSupport,
					deleteCoordinator);
		}

		private DeleteAggregateCrudService(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
	}
}