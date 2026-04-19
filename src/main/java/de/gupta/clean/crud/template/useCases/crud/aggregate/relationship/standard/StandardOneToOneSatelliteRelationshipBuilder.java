package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.builder.AggregateRelationshipDefinitions;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.ReconciliationStrategy;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class StandardOneToOneSatelliteRelationshipBuilder<
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
	private Function<MasterDomainModelCreate, Optional<SatellitePublicCreate>> createExtractor;
	private Function<SatellitePublicCreate, SatelliteDomainModelCreate> createMapper;
	private Function<MasterDomainModelUpdatePatch, Optional<SatelliteUpdatePatchItem<SatelliteDomainId,
			SatellitePublicUpdatePatch>>> patchExtractor = _ -> Optional.empty();
	private Function<SatellitePublicUpdatePatch, SatelliteDomainModelUpdatePatch> patchMapper;
	private Function<SatellitePublicUpdatePatch, SatelliteDomainModelCreate> patchCreateMapper;
	private Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor = _ -> List.of();
	private Function<MasterDomainModel, Optional<SatellitePublicResponse>> currentSatellite;
	private BiFunction<MasterDomainModel, Optional<SatellitePublicResponse>, MasterDomainModel> replaceSatellite;
	private Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
			publicResponseMapper;

	public StandardOneToOneSatelliteRelationshipBuilder(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		this.name = Objects.requireNonNull(name);
		this.satelliteDefinition = Objects.requireNonNull(satelliteDefinition);
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> lifecycleSemantics(
			final LifecycleSemantics lifecycleSemantics)
	{
		this.lifecycleSemantics = lifecycleSemantics;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> createExtractor(
			final Function<MasterDomainModelCreate, Optional<SatellitePublicCreate>> createExtractor)
	{
		this.createExtractor = createExtractor;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> createMapper(
			final Function<SatellitePublicCreate, SatelliteDomainModelCreate> createMapper)
	{
		this.createMapper = createMapper;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchExtractor(
			final Function<MasterDomainModelUpdatePatch, Optional<SatelliteUpdatePatchItem<SatelliteDomainId,
					SatellitePublicUpdatePatch>>> patchExtractor)
	{
		this.patchExtractor = patchExtractor;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchMapper(
			final Function<SatellitePublicUpdatePatch, SatelliteDomainModelUpdatePatch> patchMapper)
	{
		this.patchMapper = patchMapper;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> patchCreateMapper(
			final Function<SatellitePublicUpdatePatch, SatelliteDomainModelCreate> patchCreateMapper)
	{
		this.patchCreateMapper = patchCreateMapper;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> removeIdExtractor(
			final Function<MasterDomainModelUpdatePatch, Collection<SatelliteDomainId>> removeIdExtractor)
	{
		this.removeIdExtractor = removeIdExtractor;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> currentSatellite(
			final Function<MasterDomainModel, Optional<SatellitePublicResponse>> currentSatellite)
	{
		this.currentSatellite = currentSatellite;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> replaceSatellite(
			final BiFunction<MasterDomainModel, Optional<SatellitePublicResponse>, MasterDomainModel> replaceSatellite)
	{
		this.replaceSatellite = replaceSatellite;
		return this;
	}

	public StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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
				.cardinality(Cardinality.ONE)
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
					var patchItem = required(patchExtractor, "patchExtractor").apply(masterPatch);
					var removeIds = List.copyOf(required(removeIdExtractor, "removeIdExtractor").apply(masterPatch));
					if (patchItem.isPresent() && !removeIds.isEmpty())
					{
						throw InvalidRequestException.withMessage(
								"Relationship '%s' cannot combine a nested patch item with remove ids for ONE cardinality".formatted(
										name));
					}
					if (removeIds.size() > 1)
					{
						throw InvalidRequestException.withMessage(
								"Relationship '%s' cannot remove more than one satellite for ONE cardinality".formatted(
										name));
					}
					var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId,
							SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>();
					patchItem.ifPresent(item ->
					{
						if (item.id().isPresent())
						{
							mutationIntents.add(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(
									item.id().orElseThrow(),
									required(patchMapper, "patchMapper").apply(item.patch())));
							return;
						}
						mutationIntents.add(new SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<>(
								required(patchCreateMapper, "patchCreateMapper").apply(item.patch()),
								required(patchMapper, "patchMapper").apply(item.patch())));
					});
					removeIds.forEach(satelliteDomainId -> mutationIntents.add(
							new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(satelliteDomainId)));
					return mutationIntents;
				})
				.identityResolver((masterDomainModel, _) -> required(currentSatellite, "currentSatellite").apply(
						masterDomainModel).map(StandardSatelliteRelationshipSupport::requiredId))
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(StandardSatelliteRelationshipSupport.oneToOneLinkStrategy(
						name,
						satelliteDefinition,
						required(publicResponseMapper, "publicResponseMapper"),
						required(currentSatellite, "currentSatellite"),
						required(replaceSatellite, "replaceSatellite")))
				.hydrationStrategy(StandardSatelliteRelationshipSupport.defaultHydrationStrategy())
				.build();
	}

	private static <Value> Value required(final Value value, final String name)
	{
		return Objects.requireNonNull(value, name);
	}
}