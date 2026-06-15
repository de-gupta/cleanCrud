package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateFetchCoordinator;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractFetchService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements FetchService<MasterDomainModel, MasterDomainId>
{
	private final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateFetchCoordinator fetchCoordinator;

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll()
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(AggregateWorkflowBuilder.readOnlyFlow(() -> findAllModels(relationships)).build());
	}

	@Override
	public Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(final Pageable pageable)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findAllModels(pageable, relationships)).build());
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModel> findById(final MasterDomainId domainID)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findModelById(domainID, relationships)).build());
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(final Set<MasterDomainId> ids)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findModelsByIds(ids, relationships)).build());
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findModelsByIds(
			final Set<MasterDomainId> ids,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.beckon(relationships)
		                .coronate(List::isEmpty,
								ignored -> findModelsByIdsWithoutRelationships(ids),
								relationshipDefinitions -> findModelsByIdsWithRelationships(ids,
										relationshipDefinitions));
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findModelsByIdsWithoutRelationships(
			final Set<MasterDomainId> ids)
	{
		return definition.fetchPort().findByIds(ids).stream().filter(this::isVisible).toList();
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findModelsByIdsWithRelationships(
			final Set<MasterDomainId> ids,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findByIds(definition, relationships, ids);
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModel> findModelById(
			final MasterDomainId domainID,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findModelByIdWithoutRelationships(domainID),
								rels -> findModelByIdWithRelationships(domainID, rels));
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModel> findModelByIdWithoutRelationships(
			final MasterDomainId domainID)
	{
		return definition.fetchPort().findById(domainID)
		                 .filter(this::isVisible)
		                 .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModel> findModelByIdWithRelationships(
			final MasterDomainId domainID,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findById(definition, relationships, domainID);
	}

	private Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModels(
			final Pageable pageable,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findAllModelsWithoutRelationships(pageable),
								rels -> findAllModelsWithRelationships(pageable, rels));
	}

	private Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModelsWithoutRelationships(
			final Pageable pageable)
	{
		return PageUtility.filterSlice(definition.fetchPort().findAll(pageable), this::isVisible);
	}

	private Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModelsWithRelationships(
			final Pageable pageable,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findAll(definition, relationships, pageable);
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModels(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findAllModelsWithoutRelationships(),
								this::findAllModelsWithRelationships);
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModelsWithoutRelationships()
	{
		return definition.fetchPort().findAll().stream().filter(this::isVisible).toList();
	}

	private Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAllModelsWithRelationships(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findAll(definition, relationships);
	}

	private boolean isVisible(final IdentifiedModel<MasterDomainId, MasterDomainModel> model)
	{
		return definition.securityPolicy().isAccessAllowed(model.model());
	}

	protected AbstractFetchService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateFetchCoordinator fetchCoordinator)
	{
		this.definition = definition;
		this.engine = engine;
		this.definitionGuard = definitionGuard;
		this.fetchCoordinator = fetchCoordinator;
	}
}