package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.*;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractUpdateService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse,
		MasterDomainId>
{
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateUpdateCoordinator updateCoordinator;

	@Override
	public void putAtId(final MasterDomainId id, final MasterDomainModelCreate model)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		engine.execute(
				CrudWorkflowBuilder.writeFlow(() -> replaceModel(id, model, relationships))
				                   .startDurableProcesses(this::durableProcessStartRequests)
				                   .afterTransaction(definition.postCommitMutation())
				                   .build());
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> updateById(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return identifiedModel(engine.execute(
				CrudWorkflowBuilder.writeFlow(() -> patchModel(id, updatePatch, relationships))
				                   .startDurableProcesses(result -> durableProcessStartRequests(result.context()))
				                   .afterTransaction(this::dispatchUpdated)
				                   .build()).updated());
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return switch (mode)
		{
			case ALL_OR_NOTHING -> engine.execute(
												 CrudWorkflowBuilder.writeFlow(() -> patchAllModels(models, relationships))
					                                                .startDurableProcesses(
																			this::durableProcessStartRequests)
					                                                .afterTransaction(this::dispatchUpdated)
					                                                .build()).stream().map(UpdateDispatch::updated).map(this::identifiedModel)
			                             .toList();
			case BEST_EFFORT -> models.stream()
			                          .map(model -> tryUpdateById(model.id(), model.model()))
			                          .flatMap(Optional::stream)
			                          .map(this::identifiedModel)
			                          .toList();
		};
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
	{
		return List.of();
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> results)
	{
		return results.stream()
		              .map(UpdateDispatch::context)
		              .map(this::durableProcessStartRequests)
		              .flatMap(Collection::stream)
		              .toList();
	}

	private Optional<IdentifiedModel<MasterDomainId, MasterDomainModel>> tryUpdateById(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		try
		{
			var relationships = definitionGuard.satelliteRelationships(definition);
			return Optional.of(engine.execute(
					CrudWorkflowBuilder.writeFlow(() -> patchModel(id, updatePatch, relationships))
					                   .startDurableProcesses(result -> durableProcessStartRequests(result.context()))
					                   .afterTransaction(this::dispatchUpdated)
					                   .build()).updated());
		}
		catch (DomainException e)
		{
			return Optional.empty();
		}
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> replaceModel(
			final MasterDomainId id,
			final MasterDomainModelCreate model,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var previousModel = definition.fetchPort().findById(id).map(IdentifiedModel::model);
		Unfolding.of(relationships)
		         .coronate(List::isEmpty,
						 ignored -> replaceModelWithoutRelationships(id, model, previousModel),
						 rels -> replaceModelWithRelationships(id, model, rels));
		var currentModel = definition.fetchPort().findById(id).map(IdentifiedModel::model).orElseThrow();
		return putContext(id, previousModel, currentModel);
	}

	private Void replaceModelWithoutRelationships(
			final MasterDomainId id,
			final MasterDomainModelCreate model,
			final Optional<MasterDomainModel> previousModel)
	{
		var replacement = definition.createBuilder().toModel(model);
		validationSupport.validateAccess(definition, replacement);
		previousModel.ifPresentOrElse(
				current -> validationSupport.validateAccessAndValidatePatch(definition, current, replacement),
				() -> definition.insertionPolicy().validateInsertion(replacement));
		definition.mutationPort().put(id, replacement);
		return null;
	}

	private Void replaceModelWithRelationships(
			final MasterDomainId id,
			final MasterDomainModelCreate model,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		updateCoordinator.putAtId(definition, relationships, id, model);
		return null;
	}

	private UpdateDispatch<MasterDomainId, MasterDomainModel> patchModel(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var previousModel = definition.fetchPort()
		                              .findById(id)
		                              .map(IdentifiedModel::model)
		                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
		var updated = Unfolding.of(relationships)
		                       .coronate(List::isEmpty,
									   ignored -> patchModelWithoutRelationships(id, updatePatch),
									   rels -> updateCoordinator.updateById(definition, rels, id, updatePatch));
		return new UpdateDispatch<>(updated, patchContext(updated.id(), Optional.of(previousModel), updated.model()));
	}

	private Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> patchAllModels(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var updates = new ArrayList<UpdateDispatch<MasterDomainId, MasterDomainModel>>();
		for (var model : models)
		{
			updates.add(patchModel(model.id(), model.model(), relationships));
		}
		return updates;
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModel> patchModelWithoutRelationships(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
		validationSupport.validateAccess(definition, current.model());
		var updatedModel = definition.patcher().patchModel(current.model(), updatePatch);
		validationSupport.validateAccessAndValidatePatch(definition, current.model(), updatedModel);
		return definition.mutationPort().update(id, updatedModel);
	}

	private void dispatchUpdated(final UpdateDispatch<MasterDomainId, MasterDomainModel> result)
	{
		definition.postCommitMutation().accept(result.context());
	}

	private void dispatchUpdated(final Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> result)
	{
		result.forEach(this::dispatchUpdated);
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> putContext(
			final MasterDomainId id,
			final Optional<MasterDomainModel> previousModel,
			final MasterDomainModel currentModel)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.PUT,
				id,
				Optional.of(currentModel),
				previousModel);
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> patchContext(
			final MasterDomainId id,
			final Optional<MasterDomainModel> previousModel,
			final MasterDomainModel currentModel)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.PATCH,
				id,
				Optional.of(currentModel),
				previousModel);
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), definition.responseBuilder().toResponse(domainModel.model()));
	}

	protected AbstractUpdateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateUpdateCoordinator updateCoordinator)
	{
		this.definition = definition;
		this.engine = engine;
		this.definitionGuard = definitionGuard;
		this.validationSupport = validationSupport;
		this.updateCoordinator = updateCoordinator;
	}

	protected record UpdateDispatch<DomainId, DomainModel>(
			IdentifiedModel<DomainId, DomainModel> updated,
			PostCommitMutationContext<DomainId, DomainModel> context)
	{
	}
}