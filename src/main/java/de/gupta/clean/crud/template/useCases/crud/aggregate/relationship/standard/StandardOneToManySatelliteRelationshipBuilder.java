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

public final class StandardOneToManySatelliteRelationshipBuilder<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		SatelliteDomainId,
		SatelliteDomainModel,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch,
		SatelliteAggregateResponse,
		SatellitePublicCreate,
		SatellitePublicUpdatePatch,
		SatellitePublicResponse>
{
	private final String name;
	private final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition;
	private LifecycleSemantics lifecycleSemantics = StandardSatelliteLifecycleSemantics.idBackedDefaults();
	private ReconciliationStrategy reconciliationStrategy = ReconciliationStrategy.MERGE_BY_ID;
	private Function<MasterDomainModelCreate, Collection<SatellitePublicCreate>> createExtractor;
	private Function<SatellitePublicCreate, SatelliteDomainModelCreate> createMapper;
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteUpdatePatchItem<SatelliteDomainId,
			SatellitePublicUpdatePatch>>> patchExtractor = _ -> List.of();
	private Function<SatellitePublicUpdatePatch, SatelliteDomainModelUpdatePatch> patchMapper;
	private Function<SatellitePublicUpdatePatch, SatelliteDomainModelCreate> patchCreateMapper;
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor = _ -> List.of();
	private Function<MasterDomainModel, Collection<SatellitePublicResponse>> currentSatellites;
	private BiFunction<MasterDomainModel, Collection<SatellitePublicResponse>, MasterDomainModel> replaceSatellites;
	private Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
			publicResponseMapper;

	public StandardOneToManySatelliteRelationshipBuilder(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		this.name = Objects.requireNonNull(name);
		this.satelliteDefinition = Objects.requireNonNull(satelliteDefinition);
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> lifecycleSemantics(
			final LifecycleSemantics lifecycleSemantics)
	{
		this.lifecycleSemantics = lifecycleSemantics;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> reconciliationStrategy(
			final ReconciliationStrategy reconciliationStrategy)
	{
		this.reconciliationStrategy = reconciliationStrategy;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> createExtractor(
			final Function<MasterDomainModelCreate, Collection<SatellitePublicCreate>> createExtractor)
	{
		this.createExtractor = createExtractor;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> createMapper(
			final Function<SatellitePublicCreate, SatelliteDomainModelCreate> createMapper)
	{
		this.createMapper = createMapper;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteUpdatePatchItem<SatelliteDomainId,
					SatellitePublicUpdatePatch>>> patchExtractor)
	{
		this.patchExtractor = patchExtractor;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchMapper(
			final Function<SatellitePublicUpdatePatch, SatelliteDomainModelUpdatePatch> patchMapper)
	{
		this.patchMapper = patchMapper;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchCreateMapper(
			final Function<SatellitePublicUpdatePatch, SatelliteDomainModelCreate> patchCreateMapper)
	{
		this.patchCreateMapper = patchCreateMapper;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> removeIdExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor)
	{
		this.removeIdExtractor = removeIdExtractor;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> currentSatellites(
			final Function<MasterDomainModel, Collection<SatellitePublicResponse>> currentSatellites)
	{
		this.currentSatellites = currentSatellites;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> replaceSatellites(
			final BiFunction<MasterDomainModel, Collection<SatellitePublicResponse>, MasterDomainModel>
					replaceSatellites)
	{
		this.replaceSatellites = replaceSatellites;
		return this;
	}

	public StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> publicResponseMapper(
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
				.createInputResolver(masterCreate -> required(createExtractor, "createExtractor").apply(masterCreate)
				                                                                                 .stream()
				                                                                                 .<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>map(
																										 satellitePublicCreate ->
																												 new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
																														 required(
																																 createMapper,
																																 "createMapper").apply(
																																 satellitePublicCreate)))
				                                                                                 .toList())
				.patchInputResolver(masterPatch ->
				{
					var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId,
							SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>();
					required(patchExtractor, "patchExtractor").apply(masterPatch).forEach(item ->
					{
						if (item.id().isPresent())
						{
							mutationIntents.add(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(
									item.id().orElseThrow(),
									required(patchMapper, "patchMapper").apply(item.patch())));
							return;
						}
						mutationIntents.add(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
								required(patchCreateMapper, "patchCreateMapper").apply(item.patch())));
					});
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