package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateFetchCoordinator;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class DefaultAggregateFetchService<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
		implements AggregateFetchService<DomainId, DomainModel>
{
	private final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycle engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateFetchCoordinator fetchCoordinator;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	AggregateFetchService<DomainId, DomainModel> create(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		return new DefaultAggregateFetchService<>(definition, engine);
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> findAll()
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(AggregateWorkflowBuilder.readOnlyFlow(() -> findAllModels(relationships)).build());
	}

	@Override
	public Slice<IdentifiedModel<DomainId, DomainModel>> findAll(final Pageable pageable)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findAllModels(pageable, relationships)).build());
	}

	@Override
	public IdentifiedModel<DomainId, DomainModel> findById(final DomainId domainId)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findModelById(domainId, relationships)).build());
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> findByIds(final Set<DomainId> ids)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		return engine.execute(
				AggregateWorkflowBuilder.readOnlyFlow(() -> findModelsByIds(ids, relationships)).build());
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findModelsByIds(
			final Set<DomainId> ids,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.beckon(relationships)
		                .coronate(List::isEmpty,
								ignored -> findModelsByIdsWithoutRelationships(ids),
								rels -> findModelsByIdsWithRelationships(ids, rels));
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findModelsByIdsWithoutRelationships(
			final Set<DomainId> ids)
	{
		return definition.fetchPort().findByIds(ids).stream().filter(this::isVisible).toList();
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findModelsByIdsWithRelationships(
			final Set<DomainId> ids,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findByIds(definition, relationships, ids);
	}

	private IdentifiedModel<DomainId, DomainModel> findModelById(
			final DomainId domainId,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findModelByIdWithoutRelationships(domainId),
								rels -> findModelByIdWithRelationships(domainId, rels));
	}

	private IdentifiedModel<DomainId, DomainModel> findModelByIdWithoutRelationships(final DomainId domainId)
	{
		return definition.fetchPort().findById(domainId)
		                 .filter(this::isVisible)
		                 .orElseThrow(() -> ResourceNotFoundException.withId(domainId));
	}

	private IdentifiedModel<DomainId, DomainModel> findModelByIdWithRelationships(
			final DomainId domainId,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findById(definition, relationships, domainId);
	}

	private Slice<IdentifiedModel<DomainId, DomainModel>> findAllModels(
			final Pageable pageable,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findAllModelsWithoutRelationships(pageable),
								rels -> findAllModelsWithRelationships(pageable, rels));
	}

	private Slice<IdentifiedModel<DomainId, DomainModel>> findAllModelsWithoutRelationships(final Pageable pageable)
	{
		return PageUtility.filterSlice(definition.fetchPort().findAll(pageable), this::isVisible);
	}

	private Slice<IdentifiedModel<DomainId, DomainModel>> findAllModelsWithRelationships(
			final Pageable pageable,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findAll(definition, relationships, pageable);
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findAllModels(
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return Unfolding.of(relationships)
		                .coronate(List::isEmpty,
								ignored -> findAllModelsWithoutRelationships(),
								this::findAllModelsWithRelationships);
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findAllModelsWithoutRelationships()
	{
		return definition.fetchPort().findAll().stream().filter(this::isVisible).toList();
	}

	private Collection<IdentifiedModel<DomainId, DomainModel>> findAllModelsWithRelationships(
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return fetchCoordinator.findAll(definition, relationships);
	}

	private boolean isVisible(final IdentifiedModel<DomainId, DomainModel> model)
	{
		return definition.securityPolicy().isAccessAllowed(model.model());
	}

	private DefaultAggregateFetchService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine)
	{
		this(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.fetchCoordinator());
	}

	private DefaultAggregateFetchService(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycle engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateFetchCoordinator fetchCoordinator)
	{
		this.definition = definition;
		this.engine = engine;
		this.definitionGuard = definitionGuard;
		this.fetchCoordinator = fetchCoordinator;
	}
}