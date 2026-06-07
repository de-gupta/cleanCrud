package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.*;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDeleteService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements DeleteService<MasterDomainId>
{
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateDeleteCoordinator deleteCoordinator;

	@Override
	public void deleteById(final MasterDomainId id)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		engine.execute(
				CrudWorkflowBuilder.writeFlow(() -> deleteModel(id, relationships))
				                   .afterTransaction(definition.postCommitMutation())
				                   .build());
	}

	@Override
	public void deleteAllById(final Collection<MasterDomainId> ids, final BulkOperationMode mode)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		switch (mode)
		{
			case ALL_OR_NOTHING -> engine.execute(
					CrudWorkflowBuilder.writeFlow(() -> deleteModels(ids, relationships))
					                   .afterTransaction(this::dispatchDeleted)
					                   .build());
			case BEST_EFFORT -> ids.forEach(this::tryDeleteById);
		}
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> deleteModel(
			final MasterDomainId id,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var previousModel = definition.fetchPort().findById(id).map(IdentifiedModel::model).orElseThrow();
		Unfolding.of(relationships)
		         .coronate(List::isEmpty,
						 ignored -> deleteModelWithoutRelationships(id, previousModel),
						 rels -> deleteModelWithRelationships(id, rels));
		return deleteContext(id, previousModel);
	}

	private Void deleteModelWithoutRelationships(
			final MasterDomainId id,
			final MasterDomainModel previousModel)
	{
		validationSupport.validateDeletion(definition, previousModel);
		definition.mutationPort().delete(id);
		return null;
	}

	private Void deleteModelWithRelationships(
			final MasterDomainId id,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		deleteCoordinator.deleteById(definition, relationships, id);
		return null;
	}

	private Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> deleteModels(
			final Collection<MasterDomainId> ids,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		var deletedModels = new ArrayList<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>();
		for (var id : ids)
		{
			deletedModels.add(deleteModel(id, relationships));
		}
		return deletedModels;
	}

	private void dispatchDeleted(final Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> result)
	{
		result.forEach(definition.postCommitMutation());
	}

	private void tryDeleteById(final MasterDomainId id)
	{
		try
		{
			deleteById(id);
		}
		catch (DomainException ignored)
		{
		}
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> deleteContext(
			final MasterDomainId id,
			final MasterDomainModel previousModel)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.DELETE,
				id,
				Optional.empty(),
				Optional.of(previousModel));
	}

	protected AbstractDeleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
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