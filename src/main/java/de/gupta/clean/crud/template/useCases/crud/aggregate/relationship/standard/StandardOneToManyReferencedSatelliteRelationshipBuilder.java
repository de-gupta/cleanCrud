package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.builder.AggregateRelationshipDefinitions;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class StandardOneToManyReferencedSatelliteRelationshipBuilder<
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
	private ReconciliationStrategy reconciliationStrategy = ReconciliationStrategy.MERGE_BY_ID;
	private Function<MasterDomainModelCreate, Collection<SatelliteDomainId>> createReferenceIdsExtractor;
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> patchReferenceIdsExtractor =
			_ -> List.of();
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor = _ -> List.of();
	private Function<MasterDomainModel, Collection<SatellitePublicResponse>> currentSatellites;
	private BiFunction<MasterDomainModel, Collection<SatellitePublicResponse>, MasterDomainModel> replaceSatellites;
	private Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
			publicResponseMapper;

	public StandardOneToManyReferencedSatelliteRelationshipBuilder(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		this.name = Objects.requireNonNull(name);
		this.satelliteDefinition = Objects.requireNonNull(satelliteDefinition);
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> lifecycleSemantics(final LifecycleSemantics lifecycleSemantics)
	{
		this.lifecycleSemantics = lifecycleSemantics;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> reconciliationStrategy(final ReconciliationStrategy reconciliationStrategy)
	{
		this.reconciliationStrategy = reconciliationStrategy;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> createReferenceIdsExtractor(
			final Function<MasterDomainModelCreate, Collection<SatelliteDomainId>> createReferenceIdsExtractor)
	{
		this.createReferenceIdsExtractor = createReferenceIdsExtractor;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> patchReferenceIdsExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> patchReferenceIdsExtractor)
	{
		this.patchReferenceIdsExtractor = patchReferenceIdsExtractor;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> removeIdExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor)
	{
		this.removeIdExtractor = removeIdExtractor;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> currentSatellites(
			final Function<MasterDomainModel, Collection<SatellitePublicResponse>> currentSatellites)
	{
		this.currentSatellites = currentSatellites;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse,
			SatellitePublicResponse> replaceSatellites(
			final BiFunction<MasterDomainModel, Collection<SatellitePublicResponse>, MasterDomainModel>
					replaceSatellites)
	{
		this.replaceSatellites = replaceSatellites;
		return this;
	}

	public StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel,
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
				.cardinality(Cardinality.MANY)
				.lifecycleSemantics(required(lifecycleSemantics, "lifecycleSemantics"))
				.satelliteDefinition(satelliteDefinition)
				.createInputResolver(masterCreate -> required(
						createReferenceIdsExtractor,
						"createReferenceIdsExtractor").apply(masterCreate).stream()
				                                      .<SatelliteCreateIntent<SatelliteDomainId,
															  SatelliteDomainModelCreate>>map(
															  SatelliteCreateIntent.ReferenceSatelliteCreateIntent::new)
				                                      .toList())
				.patchInputResolver(masterPatch ->
				{
					var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId,
							SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>();
					required(patchReferenceIdsExtractor, "patchReferenceIdsExtractor").apply(masterPatch).forEach(
							satelliteDomainId -> mutationIntents.add(
									new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(satelliteDomainId)));
					required(removeIdExtractor, "removeIdExtractor").apply(masterPatch).forEach(
							satelliteDomainId -> mutationIntents.add(
									new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(satelliteDomainId)));
					return mutationIntents;
				})
				.identityResolver((masterDomainModel, _) -> required(currentSatellites, "currentSatellites").apply(
						masterDomainModel).stream().findFirst().map(StandardSatelliteRelationshipSupport::requiredId))
				.reconciliationStrategy(required(reconciliationStrategy, "reconciliationStrategy"))
				.linkStrategy(StandardSatelliteRelationshipSupport.oneToManyLinkStrategy(
						satelliteDefinition,
						required(publicResponseMapper, "publicResponseMapper"),
						required(currentSatellites, "currentSatellites"),
						required(replaceSatellites, "replaceSatellites")))
				.hydrationStrategy(StandardSatelliteRelationshipSupport.defaultHydrationStrategy())
				.build();
	}

	private static <Value> Value required(final Value value, final String name)
	{
		return Objects.requireNonNull(value, name);
	}
}