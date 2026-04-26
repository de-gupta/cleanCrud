package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.builder.AggregateRelationshipDefinitions;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class StandardOneToOneReferencedSatelliteRelationshipBuilder<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		SatelliteDomainId,
		SatelliteDomainModel,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch,
		SatelliteAggregateResponse,
		SatellitePublicResponse>
{
	private final String name;
	private final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition;
	private LifecycleSemantics lifecycleSemantics = StandardSatelliteLifecycleSemantics.referenceOnlyDefaults();
	private Function<MasterDomainModelCreate, Optional<SatelliteDomainId>> createReferenceIdExtractor;
	private Function<MasterDomainModelUpdatePatch, Optional<SatelliteDomainId>> patchReferenceIdExtractor =
			_ -> Optional.empty();
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor = _ -> List.of();
	private Function<MasterDomainModel, Optional<SatellitePublicResponse>> currentSatellite;
	private BiFunction<MasterDomainModel, Optional<SatellitePublicResponse>, MasterDomainModel> replaceSatellite;
	private Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
			publicResponseMapper;

	public StandardOneToOneReferencedSatelliteRelationshipBuilder(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		this.name = Objects.requireNonNull(name);
		this.satelliteDefinition = Objects.requireNonNull(satelliteDefinition);
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> lifecycleSemantics(final LifecycleSemantics lifecycleSemantics)
	{
		this.lifecycleSemantics = lifecycleSemantics;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> createReferenceIdExtractor(
			final Function<MasterDomainModelCreate, Optional<SatelliteDomainId>> createReferenceIdExtractor)
	{
		this.createReferenceIdExtractor = createReferenceIdExtractor;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> patchReferenceIdExtractor(
			final Function<MasterDomainModelUpdatePatch, Optional<SatelliteDomainId>> patchReferenceIdExtractor)
	{
		this.patchReferenceIdExtractor = patchReferenceIdExtractor;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> removeIdExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor)
	{
		this.removeIdExtractor = removeIdExtractor;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> currentSatellite(
			final Function<MasterDomainModel, Optional<SatellitePublicResponse>> currentSatellite)
	{
		this.currentSatellite = currentSatellite;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> replaceSatellite(
			final BiFunction<MasterDomainModel, Optional<SatellitePublicResponse>, MasterDomainModel> replaceSatellite)
	{
		this.replaceSatellite = replaceSatellite;
		return this;
	}

	public StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> publicResponseMapper(
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper)
	{
		this.publicResponseMapper = publicResponseMapper;
		return this;
	}

	public AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> build()
	{
		return AggregateRelationshipDefinitions
				.<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
						SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch>aggregateRelationshipDefinition()
				.name(name)
				.cardinality(Cardinality.ONE)
				.lifecycleSemantics(required(lifecycleSemantics, "lifecycleSemantics"))
				.satelliteDefinition(satelliteDefinition)
				.createInputResolver(masterCreate -> required(
						createReferenceIdExtractor,
						"createReferenceIdExtractor").apply(masterCreate).stream()
				                                     .<SatelliteCreateIntent<SatelliteDomainId,
															 SatelliteDomainModelCreate>>map(
															 SatelliteCreateIntent.ReferenceSatelliteCreateIntent::new)
				                                     .toList())
				.patchInputResolver(masterPatch ->
				{
					var patchReferenceId = required(
							patchReferenceIdExtractor,
							"patchReferenceIdExtractor").apply(masterPatch);
					var removeIds = List.copyOf(required(removeIdExtractor, "removeIdExtractor").apply(masterPatch));
					if (patchReferenceId.isPresent() && !removeIds.isEmpty())
					{
						throw InvalidRequestException.withMessage(
								"Relationship '%s' cannot combine a reference patch item with remove ids for ONE cardinality"
										.formatted(name));
					}
					if (removeIds.size() > 1)
					{
						throw InvalidRequestException.withMessage(
								"Relationship '%s' cannot remove more than one satellite for ONE cardinality"
										.formatted(name));
					}
					var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId,
							SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>();
					patchReferenceId.ifPresent(satelliteDomainId -> mutationIntents.add(
							new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(satelliteDomainId)));
					removeIds.forEach(satelliteDomainId -> mutationIntents.add(
							new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(satelliteDomainId)));
					return mutationIntents;
				})
				.identityResolver((masterDomainModel, _) -> required(currentSatellite, "currentSatellite").apply(
						masterDomainModel).map(StandardSatelliteResponseMapper::requiredId))
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(StandardSatelliteLinkStrategyFactory.oneToOneLinkStrategy(
						name,
						satelliteDefinition,
						required(publicResponseMapper, "publicResponseMapper"),
						required(currentSatellite, "currentSatellite"),
						required(replaceSatellite, "replaceSatellite")))
				.hydrationStrategy(StandardSatelliteHydrationStrategyFactory.defaultHydrationStrategy())
				.build();
	}

	private static <Value> Value required(final Value value, final String name)
	{
		return Objects.requireNonNull(value, name);
	}
}
