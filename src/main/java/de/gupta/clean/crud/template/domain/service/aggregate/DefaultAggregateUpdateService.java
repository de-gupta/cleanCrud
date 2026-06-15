package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateUpdateCoordinator;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DefaultAggregateUpdateService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
		implements AggregateUpdateService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
{
	private final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycle engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateUpdateCoordinator updateCoordinator;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	AggregateUpdateService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch> create(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		return new DefaultAggregateUpdateService<>(definition, engine);
	}

	@Override
	public void putAtId(
			final DomainId id,
			final DomainModelCreate model,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		engine.execute(
				AggregateWorkflowBuilder.writeFlow(() -> replaceModel(id, model, relationships))
				                        .startDurableProcesses(
												context -> durableProcessStartRequests.apply(List.of(context)))
				                        .afterTransaction(definition.postCommitMutation())
				                        .build());
	}

	@Override
	public IdentifiedModel<DomainId, DomainModel> updateById(
			final DomainId id,
			final DomainModelUpdatePatch updatePatch,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.writeFlow(() -> patchModel(id, updatePatch, relationships))
				                        .startDurableProcesses(result -> durableProcessStartRequests.apply(
												List.of(result.context())))
				                        .afterTransaction(this::dispatchUpdated)
				                        .build()).updated();
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> updateAllById(
			final Collection<IdentifiedModel<DomainId, DomainModelUpdatePatch>> models,
			final AggregateBulkOperationMode mode,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return switch (mode)
		{
			case ALL_OR_NOTHING -> engine.execute(
					AggregateWorkflowBuilder.writeFlow(() -> patchAllModels(models, relationships))
					                        .startDurableProcesses(
													results -> durableProcessStartRequests.apply(
															results.stream().map(UpdateDispatch::context).toList()))
					                        .afterTransaction(this::dispatchUpdated)
					                        .build()).stream().map(UpdateDispatch::updated).toList();
			case BEST_EFFORT -> models.stream()
			                          .map(model -> tryUpdateById(model.id(), model.model(),
											  durableProcessStartRequests))
			                          .flatMap(Optional::stream)
			                          .toList();
		};
	}

	private PostCommitMutationContext<DomainId, DomainModel> replaceModel(
			final DomainId id,
			final DomainModelCreate model,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
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
			final DomainId id,
			final DomainModelCreate model,
			final Optional<DomainModel> previousModel)
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
			final DomainId id,
			final DomainModelCreate model,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		updateCoordinator.putAtId(definition, relationships, id, model);
		return null;
	}

	private PostCommitMutationContext<DomainId, DomainModel> putContext(
			final DomainId id,
			final Optional<DomainModel> previousModel,
			final DomainModel currentModel)
	{
		return new PostCommitMutationContext<>(PostCommitMutationKind.PUT, id, Optional.of(currentModel),
				previousModel);
	}

	private UpdateDispatch<DomainId, DomainModel> patchModel(
			final DomainId id,
			final DomainModelUpdatePatch updatePatch,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
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

	private void dispatchUpdated(final UpdateDispatch<DomainId, DomainModel> result)
	{
		definition.postCommitMutation().accept(result.context());
	}

	private IdentifiedModel<DomainId, DomainModel> patchModelWithoutRelationships(
			final DomainId id,
			final DomainModelUpdatePatch updatePatch)
	{
		var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
		validationSupport.validateAccess(definition, current.model());
		var updatedModel = definition.patcher().patchModel(current.model(), updatePatch);
		validationSupport.validateAccessAndValidatePatch(definition, current.model(), updatedModel);
		return definition.mutationPort().update(id, updatedModel);
	}

	private PostCommitMutationContext<DomainId, DomainModel> patchContext(
			final DomainId id,
			final Optional<DomainModel> previousModel,
			final DomainModel currentModel)
	{
		return new PostCommitMutationContext<>(PostCommitMutationKind.PATCH, id, Optional.of(currentModel),
				previousModel);
	}

	private Optional<IdentifiedModel<DomainId, DomainModel>> tryUpdateById(
			final DomainId id,
			final DomainModelUpdatePatch updatePatch,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		try
		{
			return Optional.of(updateById(id, updatePatch, durableProcessStartRequests));
		}
		catch (DomainException e)
		{
			return Optional.empty();
		}
	}

	private Collection<UpdateDispatch<DomainId, DomainModel>> patchAllModels(
			final Collection<IdentifiedModel<DomainId, DomainModelUpdatePatch>> models,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var updates = new ArrayList<UpdateDispatch<DomainId, DomainModel>>();
		for (var model : models)
		{
			updates.add(patchModel(model.id(), model.model(), relationships));
		}
		return updates;
	}

	private void dispatchUpdated(final Collection<UpdateDispatch<DomainId, DomainModel>> result)
	{
		result.forEach(this::dispatchUpdated);
	}

	private DefaultAggregateUpdateService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		this(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.updateCoordinator());
	}

	private DefaultAggregateUpdateService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine,
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

	private record UpdateDispatch<DomainId, DomainModel>(
			IdentifiedModel<DomainId, DomainModel> updated,
			PostCommitMutationContext<DomainId, DomainModel> context)
	{
	}
}