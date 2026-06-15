package de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AggregateFetchCoordinator
{
	public static AggregateFetchCoordinator create()
	{
		return new AggregateFetchCoordinator();
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships)
	{
		return visibleHydratedModels(definition, relationships, definition.fetchPort().findAll());
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> List<IdentifiedModel<MasterDomainId, MasterDomainModel>> visibleHydratedModels(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> identifiedModels)
	{
		return identifiedModels.stream()
		                       .map(model -> visibleHydratedModel(definition, relationships, model))
		                       .flatMap(Optional::stream)
		                       .toList();
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Optional<IdentifiedModel<MasterDomainId, MasterDomainModel>> visibleHydratedModel(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(identifiedModel.model()))
		{
			return Optional.empty();
		}
		return Optional.of(hydrate(relationships, identifiedModel));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	IdentifiedModel<MasterDomainId, MasterDomainModel> hydrate(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		MasterDomainModel hydratedModel = identifiedModel.model();
		for (var relationship : relationships)
		{
			if (!relationship.lifecycleSemantics().hydrateOnFetch())
			{
				continue;
			}
			hydratedModel = applyHydration(relationship, identifiedModel.id(), hydratedModel);
		}
		return IdentifiedModel.of(identifiedModel.id(), hydratedModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyHydration(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainId masterDomainId,
			final MasterDomainModel masterDomainModel)
	{
		return relationship.hydrationStrategy()
		                   .hydrate(
								   IdentifiedModel.of(masterDomainId, masterDomainModel),
								   relationship.satelliteFetchPort(),
								   relationship.linkStrategy());
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Pageable pageable)
	{
		var sourceSlice = definition.fetchPort().findAll(pageable);
		return new SliceImpl<>(
				visibleHydratedModels(definition, relationships, sourceSlice.getContent()),
				sourceSlice.getPageable(),
				sourceSlice.hasNext());
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> findById(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId)
	{
		return definition.fetchPort()
		                 .findById(masterDomainId)
		                 .flatMap(model -> visibleHydratedModel(definition, relationships, model))
		                 .orElseThrow(() -> ResourceNotFoundException.withId(masterDomainId));
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Set<MasterDomainId> masterDomainIds)
	{
		return visibleHydratedModels(definition, relationships, definition.fetchPort().findByIds(masterDomainIds));
	}

	private AggregateFetchCoordinator()
	{
	}
}