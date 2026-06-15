package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.*;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractSaveService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
{
	private final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateSaveCoordinator saveCoordinator;

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> save(final MasterDomainModelCreate model)
	{
		return saveAll(List.of(model)).stream().findFirst().orElseThrow();
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> saveAll(
			final Collection<MasterDomainModelCreate> models)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
							 AggregateWorkflowBuilder.writeFlow(() -> persistAll(models, relationships))
				                                     .startDurableProcesses(this::durableProcessStartRequests)
				                                     .afterTransaction(this::dispatchCreated)
				                                     .build())
		             .stream()
		             .map(this::identifiedModel)
		             .toList();
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> persistAll(
			final Collection<MasterDomainModelCreate> models,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> persistAllWithoutRelationships(models),
								rels -> persistAllWithRelationships(models, rels));
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> savedModels)
	{
		return List.of();
	}

	private void dispatchCreated(final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> savedModels)
	{
		savedModels.forEach(saved -> definition.postCommitMutation().accept(createContext(saved)));
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		return IdentifiedModel.of(identifiedModel.id(),
				definition.responseBuilder().toResponse(identifiedModel.model()));
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> persistAllWithoutRelationships(
			final Collection<MasterDomainModelCreate> models)
	{
		var domainModels = models.stream().map(definition.createBuilder()::toModel).toList();
		validationSupport.validateSaveModels(definition, domainModels);
		return domainModels.stream().map(definition.mutationPort()::create).toList();
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> persistAllWithRelationships(
			final Collection<MasterDomainModelCreate> models,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		validateSaveModels(models);
		return saveCoordinator.saveAll(definition, relationships, models);
	}

	private PostCommitMutationContext<MasterDomainId, MasterDomainModel> createContext(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> saved)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				saved.id(),
				Optional.of(saved.model()),
				Optional.empty());
	}

	private void validateSaveModels(final Collection<MasterDomainModelCreate> models)
	{
		validationSupport.validateSaveModels(definition,
				models.stream().map(definition.createBuilder()::toModel).toList());
	}

	protected AbstractSaveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport,
			final AggregateSaveCoordinator saveCoordinator)
	{
		this.definition = definition;
		this.engine = engine;
		this.definitionGuard = definitionGuard;
		this.validationSupport = validationSupport;
		this.saveCoordinator = saveCoordinator;
	}
}