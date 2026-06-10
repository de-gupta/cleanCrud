package de.gupta.clean.crud.template.useCases.operation.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.SatelliteReferenceResolver;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.SatelliteRelationshipPlanner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.plan.AggregateMutationPlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class AggregateMutationCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;
	private final AggregateMutationValidationSupport validationSupport;

	static AggregateMutationCoordinator with(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final AggregateMutationValidationSupport validationSupport)
	{
		return new AggregateMutationCoordinator(relationshipPlanner, referenceResolver, validationSupport);
	}

	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	MasterDomainModel applyPlan(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel currentMasterDomainModel,
			final AggregateMutationPlan<MasterDomainModel> plan,
			final OperationSource source)
	{
		var linkedMasterDomainModel = preserveExistingRelationshipLinks(
				relationships,
				currentMasterDomainModel,
				plan.updatedRoot().orElse(currentMasterDomainModel));
		for (var relationship : relationships)
		{
			var mutationIntents = plan.relationshipMutations().get(relationship.name());
			if (mutationIntents == null || mutationIntents.isEmpty())
			{
				continue;
			}
			requireOwnedRelationship(relationship);
			linkedMasterDomainModel = applyOwnedRelationshipMutation(
					relationship,
					currentMasterDomainModel,
					linkedMasterDomainModel,
					mutationIntents,
					source);
		}
		validateNoUnknownRelationshipMutations(relationships, plan);
		validatePatch(definition, source, currentMasterDomainModel, linkedMasterDomainModel);
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	MasterDomainModel preserveExistingRelationshipLinks(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel)
	{
		var linkedMasterDomainModel = updatedMasterDomainModel;
		for (var relationship : relationships)
		{
			linkedMasterDomainModel = preserveExistingRelationshipLink(
					relationship,
					currentMasterDomainModel,
					linkedMasterDomainModel);
		}
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel preserveExistingRelationshipLink(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel)
	{
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				updatedMasterDomainModel,
				relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	void validateNoUnknownRelationshipMutations(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final AggregateMutationPlan<MasterDomainModel> plan)
	{
		var knownRelationshipNames = relationships.stream().map(AggregateRelationshipDefinition::name).collect(
				java.util.stream.Collectors.toSet());
		for (var relationshipName : plan.relationshipMutations().keySet())
		{
			if (!knownRelationshipNames.contains(relationshipName))
			{
				throw InvalidRequestException.withMessage(
						"Unknown relationship '%s' in aggregate mutation plan".formatted(relationshipName));
			}
		}
	}

	private void requireOwnedRelationship(final AggregateRelationshipDefinition<?, ?, ?, ?, ?, ?, ?, ?> relationship)
	{
		if (relationship.relationshipKind() == RelationshipKind.REFERENCED)
		{
			throw InvalidRequestException.withMessage(
					"Aggregate mutation service does not support mutating referenced relationship '%s'; mutate the referenced aggregate directly".formatted(
							relationship.name()));
		}
	}

	@SuppressWarnings("unchecked")
	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyOwnedRelationshipMutation(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final Collection<SatelliteMutationIntent<?, ?, ?>> mutationIntents,
			final OperationSource source)
	{
		var typedMutationIntents =
				new ArrayList<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>();
		for (var mutationIntent : mutationIntents)
		{
			typedMutationIntents.add(
					(SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>) mutationIntent);
		}
		if (typedMutationIntents.isEmpty())
		{
			return updatedMasterDomainModel;
		}
		validateUpdateParticipation(relationship, typedMutationIntents);
		return relationship.reconciliationStrategy() == de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy.REPLACE
				? applyReplaceUpdateRelationship(
				relationship,
				currentMasterDomainModel,
				updatedMasterDomainModel,
				typedMutationIntents,
				source)
				: applyMergeByIdUpdateRelationship(
				relationship,
				currentMasterDomainModel,
				updatedMasterDomainModel,
				typedMutationIntents,
				source);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateUpdateParticipation(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>> mutationIntents)
	{
		if (!relationship.lifecycleSemantics().cascadeUpdate())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' does not allow satellite update participation".formatted(relationship.name()));
		}
		validateCurrentMutationIntentSupport(relationship, mutationIntents);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyReplaceUpdateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
					mutationIntents,
			final OperationSource source)
	{
		var state = ReplaceUpdateState.of(currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel));
		for (var mutationIntent : mutationIntents)
		{
			handleReplaceMutationIntent(relationship, state, mutationIntent, source);
		}
		deleteOrphanedSatellitesIfNeeded(relationship, difference(state.current(), state.target()));
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(relationship, updatedMasterDomainModel,
				state.target());
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyMergeByIdUpdateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
					mutationIntents,
			final OperationSource source)
	{
		var state = MergeByIdUpdateState.of(currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel));
		for (var mutationIntent : mutationIntents)
		{
			handleMergeByIdMutationIntent(relationship, state, mutationIntent, source);
		}
		deleteOrphanedSatellitesIfNeeded(relationship, state.removed());
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				updatedMasterDomainModel,
				new ArrayList<>(state.target()));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void handleReplaceMutationIntent(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final ReplaceUpdateState<SatelliteDomainId> state,
			final SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
					mutationIntent,
			final OperationSource source)
	{
		switch (mutationIntent)
		{
			case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> referenceIntent ->
					state.keep(referenceSatellite(relationship, referenceIntent.satelliteDomainId()));
			case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> createIntent ->
					state.keep(createSatellite(relationship, createIntent.satelliteDomainModelCreate(), source));
			case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateIntent -> state.keep(updateSatelliteAndKeepId(
					relationship,
					state.current(),
					updateIntent.satelliteDomainId(),
					updateIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> upsertCurrentIntent -> state.keep(upsertCurrentSatellite(
					relationship,
					state.current(),
					upsertCurrentIntent.satelliteDomainModelCreate(),
					upsertCurrentIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateCurrentIntent -> state.keep(updateCurrentSatellite(
					relationship,
					state.current(),
					updateCurrentIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ ->
			{
			}
			case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ ->
					requireOrphanDeletableCurrentSatellite(relationship, state.current());
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void handleMergeByIdMutationIntent(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MergeByIdUpdateState<SatelliteDomainId> state,
			final SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
					mutationIntent,
			final OperationSource source)
	{
		switch (mutationIntent)
		{
			case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> referenceIntent ->
					state.keep(referenceSatellite(relationship, referenceIntent.satelliteDomainId()));
			case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> createIntent ->
					state.keep(createSatellite(relationship, createIntent.satelliteDomainModelCreate(), source));
			case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateIntent -> state.keep(updateSatelliteAndKeepId(
					relationship,
					state.current(),
					updateIntent.satelliteDomainId(),
					updateIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> upsertCurrentIntent -> state.keep(upsertCurrentSatellite(
					relationship,
					state.current(),
					upsertCurrentIntent.satelliteDomainModelCreate(),
					upsertCurrentIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateCurrentIntent -> state.keep(updateCurrentSatellite(
					relationship,
					state.current(),
					updateCurrentIntent.satelliteDomainModelUpdatePatch(),
					source));
			case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> removeIntent -> state.remove(
					removeLinkedSatellite(relationship, state.current(), removeIntent.satelliteDomainId()));
			case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ -> state.remove(
					requiredCurrentLinkedSatelliteDomainId(relationship, state.current(), "remove"));
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> currentLinkedSatelliteDomainIds(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel)
	{
		return relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void requireOrphanDeletableCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds)
	{
		if (!relationship.lifecycleSemantics().orphanDelete())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' cannot remove the current satellite under REPLACE unless orphanDelete is enabled".formatted(
							relationship.name()));
		}
		requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "remove");
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId referenceSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId updateSatelliteAndKeepId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch,
			final OperationSource source)
	{
		requireCurrentlyLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, satelliteDomainId, "update");
		updateSatellite(relationship, satelliteDomainId, satelliteDomainModelUpdatePatch, source);
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId upsertCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainModelCreate satelliteDomainModelCreate,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch,
			final OperationSource source)
	{
		if (currentSatelliteDomainIds.isEmpty())
		{
			return createSatellite(relationship, satelliteDomainModelCreate, source);
		}
		return updateCurrentSatellite(relationship, currentSatelliteDomainIds, satelliteDomainModelUpdatePatch,
				source);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId updateCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch,
			final OperationSource source)
	{
		var currentSatelliteDomainId =
				requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
		updateSatellite(relationship, currentSatelliteDomainId, satelliteDomainModelUpdatePatch, source);
		return currentSatelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId removeLinkedSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId)
	{
		requireCurrentlyLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, satelliteDomainId, "remove");
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateCurrentMutationIntentSupport(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>> mutationIntents)
	{
		if (relationship.cardinality() != Cardinality.MANY)
		{
			return;
		}
		if (mutationIntents.stream().anyMatch(mutationIntent -> mutationIntent
				instanceof SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<?, ?, ?>
				|| mutationIntent instanceof SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<?, ?, ?>
				|| mutationIntent instanceof SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<?, ?, ?>))
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' cannot use implicit current satellite mutations for MANY cardinality; use explicit satellite ids instead".formatted(
							relationship.name()));
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId createSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModelCreate satelliteDomainModelCreate,
			final OperationSource source)
	{
		if (!relationship.lifecycleSemantics().cascadeCreate())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' does not allow satellite create participation".formatted(relationship.name()));
		}
		var satelliteDomainModel =
				relationship.satelliteDefinition().createBuilder().toModel(satelliteDomainModelCreate);
		validateSatelliteInsertion(relationship, satelliteDomainModel, source);
		return relationship.satelliteDefinition().mutationPort().create(satelliteDomainModel).id();
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void updateSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch,
			final OperationSource source)
	{
		var currentSatellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		var updatedSatelliteDomainModel =
				relationship.satelliteDefinition().patcher().patchModel(currentSatellite.model(),
						satelliteDomainModelUpdatePatch);
		validateSatellitePatch(relationship, currentSatellite.model(), updatedSatelliteDomainModel, source);
		relationship.satelliteDefinition().mutationPort().update(satelliteDomainId, updatedSatelliteDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void deleteOrphanedSatellitesIfNeeded(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> removedSatelliteDomainIds)
	{
		if (!relationship.lifecycleSemantics().orphanDelete())
		{
			return;
		}
		for (var satelliteDomainId : removedSatelliteDomainIds)
		{
			deleteSatellite(relationship, satelliteDomainId);
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void deleteSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		var currentSatellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		relationship.satelliteDefinition().deletionPolicy().validateDeletion(currentSatellite.model());
		relationship.satelliteDefinition().mutationPort().delete(satelliteDomainId);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validatePatch(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final OperationSource source,
			final MasterDomainModel originalMasterDomainModel,
			final MasterDomainModel replacementMasterDomainModel)
	{
		validationSupport.validateSourceAwarePatch(
				definition,
				source,
				originalMasterDomainModel,
				replacementMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatelliteInsertion(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel satelliteDomainModel,
			final OperationSource source)
	{
		if (!relationship.satelliteDefinition().mutationAccessPolicy()
		                 .accessViolationFor(source, satelliteDomainModel, satelliteDomainModel)
		                 .isEmpty())
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		relationship.satelliteDefinition().insertionPolicy().validateInsertion(satelliteDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatellitePatch(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel originalSatelliteDomainModel,
			final SatelliteDomainModel replacementSatelliteDomainModel,
			final OperationSource source)
	{
		validationSupport.validateSourceAwarePatch(
				relationship.satelliteDefinition(),
				source,
				originalSatelliteDomainModel,
				replacementSatelliteDomainModel);
	}

	private <DomainId> List<DomainId> difference(final Collection<DomainId> left, final Collection<DomainId> right)
	{
		var difference = new ArrayList<>(left);
		difference.removeAll(right);
		return difference;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void requireCurrentlyLinkedSatelliteDomainId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId,
			final String operation)
	{
		if (!currentSatelliteDomainIds.contains(satelliteDomainId))
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' cannot %s a satellite that is not currently linked".formatted(
							relationship.name(),
							operation));
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId requiredCurrentLinkedSatelliteDomainId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final String operation)
	{
		if (currentSatelliteDomainIds.isEmpty())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' cannot %s because no satellite is currently linked".formatted(
							relationship.name(),
							operation));
		}
		if (currentSatelliteDomainIds.size() > 1)
		{
			throw InvalidRequestException.withMessage(
					"Relationship '%s' cannot %s implicitly because more than one satellite is currently linked".formatted(
							relationship.name(),
							operation));
		}
		return currentSatelliteDomainIds.iterator().next();
	}

	private AggregateMutationCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final AggregateMutationValidationSupport validationSupport)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
		this.validationSupport = validationSupport;
	}

	private record ReplaceUpdateState<SatelliteDomainId>(List<SatelliteDomainId> current,
	                                                     List<SatelliteDomainId> target)
	{
		private static <SatelliteDomainId> ReplaceUpdateState<SatelliteDomainId> of(
				final List<SatelliteDomainId> current)
		{
			return new ReplaceUpdateState<>(current, new ArrayList<>());
		}

		private void keep(final SatelliteDomainId satelliteDomainId)
		{
			target.add(satelliteDomainId);
		}
	}

	private record MergeByIdUpdateState<SatelliteDomainId>(
			List<SatelliteDomainId> current,
			LinkedHashSet<SatelliteDomainId> target,
			LinkedHashSet<SatelliteDomainId> removed)
	{
		private static <SatelliteDomainId> MergeByIdUpdateState<SatelliteDomainId> of(
				final List<SatelliteDomainId> current)
		{
			return new MergeByIdUpdateState<>(current, new LinkedHashSet<>(current), new LinkedHashSet<>());
		}

		private void keep(final SatelliteDomainId satelliteDomainId)
		{
			target.add(satelliteDomainId);
		}

		private void remove(final SatelliteDomainId satelliteDomainId)
		{
			target.remove(satelliteDomainId);
			removed.add(satelliteDomainId);
		}
	}
}