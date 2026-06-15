package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDeleteCoordinator;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateServiceSupportFactory;
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

public final class DefaultAggregateDeleteService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
		implements AggregateDeleteService<DomainId, DomainModel>
{
	private final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycle engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateDeleteCoordinator deleteCoordinator;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	AggregateDeleteService<DomainId, DomainModel> create(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		return new DefaultAggregateDeleteService<>(definition, engine);
	}

	@Override
	public void deleteById(
			final DomainId id,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		engine.execute(
				AggregateWorkflowBuilder.writeFlow(() -> deleteModel(id, relationships))
				                        .startDurableProcesses(
												context -> durableProcessStartRequests.apply(List.of(context)))
				                        .afterTransaction(definition.postCommitMutation())
				                        .build());
	}

	@Override
	public void deleteAllById(
			final Collection<DomainId> ids,
			final AggregateBulkOperationMode mode,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		switch (mode)
		{
			case ALL_OR_NOTHING -> engine.execute(
					AggregateWorkflowBuilder.writeFlow(() -> deleteModels(ids, relationships))
					                        .startDurableProcesses(durableProcessStartRequests)
					                        .afterTransaction(this::dispatchDeleted)
					                        .build());
			case BEST_EFFORT -> ids.forEach(id -> tryDeleteById(id, durableProcessStartRequests));
		}
	}

	private PostCommitMutationContext<DomainId, DomainModel> deleteModel(
			final DomainId id,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var previousModel = definition.fetchPort()
		                              .findById(id)
		                              .map(IdentifiedModel::model)
		                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
		Unfolding.of(relationships)
		         .coronate(List::isEmpty,
						 ignored -> deleteModelWithoutRelationships(id, previousModel),
						 rels -> deleteModelWithRelationships(id, rels));
		return deleteContext(id, previousModel);
	}

	private Void deleteModelWithoutRelationships(final DomainId id, final DomainModel previousModel)
	{
		validationSupport.validateDeletion(definition, previousModel);
		definition.mutationPort().delete(id);
		return null;
	}

	private Void deleteModelWithRelationships(
			final DomainId id,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		deleteCoordinator.deleteById(definition, relationships, id);
		return null;
	}

	private Collection<PostCommitMutationContext<DomainId, DomainModel>> deleteModels(
			final Collection<DomainId> ids,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var deletedModels = new ArrayList<PostCommitMutationContext<DomainId, DomainModel>>();
		for (var id : ids)
		{
			deletedModels.add(deleteModel(id, relationships));
		}
		return deletedModels;
	}

	private void dispatchDeleted(final Collection<PostCommitMutationContext<DomainId, DomainModel>> result)
	{
		result.forEach(definition.postCommitMutation());
	}

	private void tryDeleteById(
			final DomainId id,
			final Function<Collection<PostCommitMutationContext<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		try
		{
			deleteById(id, durableProcessStartRequests);
		}
		catch (DomainException ignored)
		{
		}
	}

	private PostCommitMutationContext<DomainId, DomainModel> deleteContext(
			final DomainId id,
			final DomainModel previousModel)
	{
		return new PostCommitMutationContext<>(PostCommitMutationKind.DELETE, id, Optional.empty(),
				Optional.of(previousModel));
	}

	private DefaultAggregateDeleteService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		this(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.deleteCoordinator());
	}

	private DefaultAggregateDeleteService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateDeleteCoordinator deleteCoordinator)
	{
		this.definition = definition;
		this.engine = engine;
		this.definitionGuard = definitionGuard;
		this.validationSupport = validationSupport;
		this.deleteCoordinator = deleteCoordinator;
	}
}