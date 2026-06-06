package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.*;

public final class DefaultAggregateLifecycleEngine implements AggregateLifecycleEngine
{
	private final PersistenceTransactionRunner transactionRunner;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateSaveCoordinator saveCoordinator;
	private final AggregateFetchCoordinator fetchCoordinator;
	private final AggregateDeleteCoordinator deleteCoordinator;
	private final AggregateUpdateCoordinator updateCoordinator;
	private final PostCommitMutationDispatcher postCommitMutationDispatcher;

	public static DefaultAggregateLifecycleEngine withTransactionRunner(
			final PersistenceTransactionRunner transactionRunner)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, PostCommitMutationDispatcher.async());
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerAndDispatcher(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, postCommitMutationDispatcher);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> save(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModelCreate model)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var saved = transactionRunner.inTransaction(() ->
		{
			if (relationships.isEmpty())
			{
				return saveAllWithoutRelationships(definition, List.of(model)).stream().findFirst().orElseThrow();
			}
			return saveCoordinator.saveAll(definition, relationships, List.of(model)).stream().findFirst()
			                      .orElseThrow();
		});
		dispatch(definition, createContext(saved));
		return saved;
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<MasterDomainModelCreate> models)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var savedModels = transactionRunner.inTransaction(() ->
		{
			if (relationships.isEmpty())
			{
				return saveAllWithoutRelationships(definition, models);
			}
			validateSaveModels(definition, models.stream().map(definition.createBuilder()::toModel).toList());
			return saveCoordinator.saveAll(definition, relationships, models);
		});
		savedModels.forEach(savedModel -> dispatch(definition, createContext(savedModel)));
		return savedModels;
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putAtId(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id,
			final MasterDomainModelCreate model)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var context = transactionRunner.inTransaction(() ->
		{
			var previousModel = definition.fetchPort().findById(id).map(IdentifiedModel::model);
			if (relationships.isEmpty())
			{
				putAtIdWithoutRelationships(definition, id, model);
			}
			else
			{
				updateCoordinator.putAtId(definition, relationships, id, model);
			}
			var currentModel = definition.fetchPort().findById(id)
			                             .map(IdentifiedModel::model)
			                             .orElseThrow(() -> ResourceNotFoundException.withId(id));
			return putContext(id, previousModel, currentModel);
		});
		dispatch(definition, context);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> updateById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var updateResult = transactionRunner.inTransaction(() ->
		{
			var previousModel = definition.fetchPort().findById(id)
			                              .map(IdentifiedModel::model)
			                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
			var updated = relationships.isEmpty()
					? updateByIdWithoutRelationships(definition, id, updatePatch)
					: updateCoordinator.updateById(definition, relationships, id, updatePatch);
			return new UpdateDispatch<>(updated,
					patchContext(updated.id(), Optional.of(previousModel), updated.model()));
		});
		dispatch(definition, updateResult.context());
		return updateResult.updated();
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> updateAllById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return switch (mode)
		{
			case ALL_OR_NOTHING ->
			{
				var updates = transactionRunner.inTransaction(() -> models.stream()
				                                                          .map(model ->
																		  {
																			  var previousModel = definition.fetchPort()
					                                                                                        .findById(
																													model.id())
					                                                                                        .map(IdentifiedModel::model)
					                                                                                        .orElseThrow(
																													() -> ResourceNotFoundException.withId(
																															model.id()));
																			  var updated = relationships.isEmpty()
																					  ? updateByIdWithoutRelationships(
																					  definition,
																					  model.id(),
																					  model.model())
																					  : updateCoordinator.updateById(
																					  definition,
																					  relationships,
																					  model.id(),
																					  model.model());
																			  return new UpdateDispatch<>(
																					  updated,
																					  patchContext(updated.id(),
																							  Optional.of(
																									  previousModel),
																							  updated.model()));
																		  })
				                                                          .toList());
				var updatedModels = new ArrayList<IdentifiedModel<MasterDomainId, MasterDomainModel>>();
				for (var update : updates)
				{
					updatedModels.add(update.updated());
					dispatch(definition, update.context());
				}
				yield updatedModels;
			}
			case BEST_EFFORT -> models.stream()
			                          .map(model -> tryUpdateById(definition, relationships, model.id(), model.model()))
			                          .flatMap(Optional::stream)
			                          .toList();
		};
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		if (relationships.isEmpty())
		{
			return definition.fetchPort().findAll().stream().filter(model -> isVisible(definition, model)).toList();
		}
		return fetchCoordinator.findAll(definition, relationships);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Pageable pageable)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		if (relationships.isEmpty())
		{
			return PageUtility.filterSlice(definition.fetchPort().findAll(pageable),
					model -> isVisible(definition, model));
		}
		return fetchCoordinator.findAll(definition, relationships, pageable);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> findById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		if (relationships.isEmpty())
		{
			return definition.fetchPort().findById(id)
			                 .filter(model -> isVisible(definition, model))
			                 .orElseThrow(() -> ResourceNotFoundException.withId(id));
		}
		return fetchCoordinator.findById(definition, relationships, id);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Set<MasterDomainId> ids)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		if (relationships.isEmpty())
		{
			return definition.fetchPort().findByIds(ids).stream().filter(model -> isVisible(definition, model))
			                 .toList();
		}
		return fetchCoordinator.findByIds(definition, relationships, ids);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void deleteById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var context = transactionRunner.inTransaction(() ->
		{
			var previousModel = definition.fetchPort().findById(id)
			                              .map(IdentifiedModel::model)
			                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
			if (relationships.isEmpty())
			{
				deleteByIdWithoutRelationships(definition, id);
			}
			else
			{
				deleteCoordinator.deleteById(definition, relationships, id);
			}
			return deleteContext(id, previousModel);
		});
		dispatch(definition, context);
	}

	@Override
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void deleteAllById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<MasterDomainId> ids,
			final BulkOperationMode mode)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		switch (mode)
		{
			case ALL_OR_NOTHING ->
			{
				var deletedModels = transactionRunner.inTransaction(() -> ids.stream()
				                                                             .map(id ->
																			 {
																				 var previousModel =
																						 definition.fetchPort()
							                                                                       .findById(id)
							                                                                       .map(IdentifiedModel::model)
							                                                                       .orElseThrow(
																										   () -> ResourceNotFoundException.withId(
																												   id));
																				 if (relationships.isEmpty())
																				 {
																					 deleteByIdWithoutRelationships(
																							 definition, id);
																				 }
																				 else
																				 {
																					 deleteCoordinator.deleteById(
																							 definition, relationships,
																							 id);
																				 }
																				 return deleteContext(id,
																						 previousModel);
																			 })
				                                                             .toList());
				deletedModels.forEach(context -> dispatch(definition, context));
			}
			case BEST_EFFORT -> ids.forEach(id -> tryDeleteById(definition, relationships, id));
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>>
	saveAllWithoutRelationships(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<MasterDomainModelCreate> models)
	{
		var domainModels = models.stream().map(definition.createBuilder()::toModel).toList();
		validateSaveModels(definition, domainModels);
		return domainModels.stream().map(definition.mutationPort()::create).toList();
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putAtIdWithoutRelationships(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id,
			final MasterDomainModelCreate model)
	{
		var replacement = definition.createBuilder().toModel(model);
		validateAccess(definition, replacement);
		definition.fetchPort().findById(id).map(IdentifiedModel::model).ifPresentOrElse(
				current -> validateAccessAndValidatePatch(definition, current, replacement),
				() -> definition.insertionPolicy().validateInsertion(replacement));
		definition.mutationPort().put(id, replacement);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> updateByIdWithoutRelationships(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
		validateAccess(definition, current.model());
		var updatedModel = definition.patcher().patchModel(current.model(), updatePatch);
		validateAccessAndValidatePatch(definition, current.model(), updatedModel);
		return definition.mutationPort().update(id, updatedModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Optional<IdentifiedModel<MasterDomainId, MasterDomainModel>> tryUpdateById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		try
		{
			var updateResult = transactionRunner.inTransaction(() ->
			{
				var previousModel = definition.fetchPort().findById(id)
				                              .map(IdentifiedModel::model)
				                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
				var updated = relationships.isEmpty()
						? updateByIdWithoutRelationships(definition, id, updatePatch)
						: updateCoordinator.updateById(definition, relationships, id, updatePatch);
				return new UpdateDispatch<>(updated,
						patchContext(updated.id(), Optional.of(previousModel), updated.model()));
			});
			dispatch(definition, updateResult.context());
			return Optional.of(updateResult.updated());
		}
		catch (DomainException e)
		{
			return Optional.empty();
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void deleteByIdWithoutRelationships(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId id)
	{
		var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
		validateAccess(definition, current.model());
		definition.deletionPolicy().validateDeletion(current.model());
		definition.mutationPort().delete(id);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void tryDeleteById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId id)
	{
		try
		{
			var context = transactionRunner.inTransaction(() ->
			{
				var previousModel = definition.fetchPort().findById(id)
				                              .map(IdentifiedModel::model)
				                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
				if (relationships.isEmpty())
				{
					deleteByIdWithoutRelationships(definition, id);
				}
				else
				{
					deleteCoordinator.deleteById(definition, relationships, id);
				}
				return deleteContext(id, previousModel);
			});
			dispatch(definition, context);
		}
		catch (DomainException ignored)
		{
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void dispatch(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final PostCommitMutationContext<MasterDomainId, MasterDomainModel> context)
	{
		postCommitMutationDispatcher.dispatch(definition.postCommitMutation(), context);
	}

	private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> createContext(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> saved)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				saved.id(),
				Optional.of(saved.model()),
				Optional.empty());
	}

	private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> putContext(
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

	private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> patchContext(
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

	private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> deleteContext(
			final MasterDomainId id,
			final MasterDomainModel previousModel)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.DELETE,
				id,
				Optional.empty(),
				Optional.of(previousModel));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateSaveModels(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<MasterDomainModel> domainModels)
	{
		if (!domainModels.stream().allMatch(definition.securityPolicy()::isAccessAllowed))
		{
			throw AccessDeniedException.withMessage("Access not allowed for one or more models");
		}
		throwIfDuplicatesInCollection(domainModels, definition.duplicateDefinition());
		domainModels.forEach(definition.insertionPolicy()::validateInsertion);
	}

	private <MasterDomainModel> void throwIfDuplicatesInCollection(
			final Collection<MasterDomainModel> models,
			final DuplicateDefinition<MasterDomainModel> duplicateDefinition)
	{
		if (duplicateDefinition instanceof KeyBasedDuplicateDefinition<MasterDomainModel, ?> keyBasedDuplicateDefinition)
		{
			throwIfDuplicateKeysInCollection(models, keyBasedDuplicateDefinition);
			return;
		}
		throwIfDuplicateDefinitionsInCollection(models, duplicateDefinition);
	}

	private <MasterDomainModel> void throwIfDuplicateDefinitionsInCollection(
			final Collection<MasterDomainModel> models,
			final DuplicateDefinition<MasterDomainModel> duplicateDefinition)
	{
		var seen = new ArrayList<MasterDomainModel>();
		for (var model : models)
		{
			if (seen.stream().anyMatch(existing -> duplicateDefinition.areDuplicates(existing, model)))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
			seen.add(model);
		}
	}

	private <MasterDomainModel> void throwIfDuplicateKeysInCollection(
			final Collection<MasterDomainModel> models,
			final KeyBasedDuplicateDefinition<MasterDomainModel, ?> keyBasedDuplicateDefinition)
	{
		var seenKeys = new HashSet<>();
		for (var model : models)
		{
			if (!seenKeys.add(keyBasedDuplicateDefinition.duplicateKeyOf(model)))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> boolean isVisible(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		return definition.securityPolicy().isAccessAllowed(identifiedModel.model());
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateAccess(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel model)
	{
		if (!definition.securityPolicy().isAccessAllowed(model))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateAccessAndValidatePatch(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel original,
			final MasterDomainModel replacement)
	{
		validateAccess(definition, original);
		validateAccess(definition, replacement);
		definition.patchPolicy().validatePatchAttempt(original, replacement);
	}

	private DefaultAggregateLifecycleEngine(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		this.transactionRunner = transactionRunner;
		this.postCommitMutationDispatcher = postCommitMutationDispatcher;
		var relationshipPlanner = new SatelliteRelationshipPlanner();
		var referenceResolver = new SatelliteReferenceResolver();
		this.definitionGuard = new AggregateDefinitionGuard();
		this.saveCoordinator = new AggregateSaveCoordinator(relationshipPlanner, referenceResolver);
		this.fetchCoordinator = new AggregateFetchCoordinator();
		this.deleteCoordinator = new AggregateDeleteCoordinator(relationshipPlanner, referenceResolver);
		this.updateCoordinator = new AggregateUpdateCoordinator(relationshipPlanner, referenceResolver);
	}

	private record UpdateDispatch<DomainId, DomainModel>(
			IdentifiedModel<DomainId, DomainModel> updated,
			PostCommitMutationContext<DomainId, DomainModel> context)
	{
	}
}