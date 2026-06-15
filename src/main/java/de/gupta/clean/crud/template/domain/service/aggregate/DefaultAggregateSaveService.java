package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.graph.AggregateDefinitionRelationshipInspector;
import de.gupta.clean.crud.template.domain.aggregate.graph.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.domain.aggregate.graph.AggregateSaveCoordinator;
import de.gupta.clean.crud.template.domain.aggregate.graph.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.aggregate.runtime.AggregateWorkflowRunner;
import de.gupta.clean.crud.template.domain.aggregate.workflow.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DefaultAggregateSaveService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
		implements AggregateSaveService<DomainId, DomainModel, DomainModelCreate>
{
	private final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateWorkflowRunner engine;
	private final AggregateDefinitionRelationshipInspector definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;
	private final AggregateSaveCoordinator saveCoordinator;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	AggregateSaveService<DomainId, DomainModel, DomainModelCreate> create(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine)
	{
		return new DefaultAggregateSaveService<>(definition, engine);
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> saveAll(
			final Collection<DomainModelCreate> models,
			final Function<Collection<IdentifiedModel<DomainId, DomainModel>>,
					Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.run(
				AggregateWorkflowBuilder.writeFlow(() -> persistAll(models, relationships))
				                        .startDurableProcesses(durableProcessStartRequests)
				                        .afterTransaction(this::dispatchCreated)
				                        .build());
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> persistAll(
			final Collection<DomainModelCreate> models,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> persistAllWithoutRelationships(models),
								rels -> persistAllWithRelationships(models, rels));
	}

	private void dispatchCreated(final Collection<IdentifiedModel<DomainId, DomainModel>> savedModels)
	{
		savedModels.forEach(saved -> definition.postCommitMutation().accept(createContext(saved)));
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> persistAllWithoutRelationships(
			final Collection<DomainModelCreate> models)
	{
		var domainModels = models.stream().map(definition.createBuilder()::toModel).toList();
		validationSupport.validateSaveModels(definition, domainModels);
		return domainModels.stream().map(definition.mutationPort()::create).toList();
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> persistAllWithRelationships(
			final Collection<DomainModelCreate> models,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		validateSaveModels(models);
		return saveCoordinator.saveAll(definition, relationships, models);
	}

	private PostCommitMutationContext<DomainId, DomainModel> createContext(
			final IdentifiedModel<DomainId, DomainModel> saved)
	{
		return new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				saved.id(),
				Optional.of(saved.model()),
				Optional.empty());
	}

	private void validateSaveModels(final Collection<DomainModelCreate> models)
	{
		validationSupport.validateSaveModels(definition,
				models.stream().map(definition.createBuilder()::toModel).toList());
	}

	private DefaultAggregateSaveService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final AggregateWorkflowRunner engine)
	{
		this(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport(),
				AggregateServiceSupportFactory.saveCoordinator());
	}

	private DefaultAggregateSaveService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine,
			final AggregateDefinitionRelationshipInspector definitionGuard,
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