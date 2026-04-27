package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.Relationship;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard.SatelliteUpdatePatchItem;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class RelationshipDrivenAggregateRelationshipDslTest
{
	@Test
	void referencedOneUsesReferenceLinkingConvention()
	{
		var relationshipDefinition = referencedOneRelationshipDefinition();

		assertThat(relationshipDefinition.cardinality()).isEqualTo(Cardinality.ONE);
		assertThat(relationshipDefinition.reconciliationStrategy()).isEqualTo(ReconciliationStrategy.REPLACE);
		assertThat(relationshipDefinition.createInputResolver().resolveSatelliteCreateIntent(
				new ReferencedOneCreate(Optional.of(11L))))
				.singleElement()
				.isInstanceOfSatisfying(
						SatelliteCreateIntent.ReferenceSatelliteCreateIntent.class,
						intent -> assertThat(intent.satelliteDomainId()).isEqualTo(11L));
		assertThat(relationshipDefinition.patchInputResolver().resolveSatelliteMutationIntents(
				new ReferencedOnePatch(Optional.of(13L), List.of())))
				.singleElement()
				.isInstanceOfSatisfying(
						SatelliteMutationIntent.ReferenceSatelliteMutationIntent.class,
						intent -> assertThat(intent.satelliteDomainId()).isEqualTo(13L));
	}

	@Test
	void referencedManyUsesRelationshipReconciliation()
	{
		var relationshipDefinition = referencedManyRelationshipDefinition(ReconciliationStrategy.MERGE_BY_ID);

		assertThat(relationshipDefinition.cardinality()).isEqualTo(Cardinality.MANY);
		assertThat(relationshipDefinition.reconciliationStrategy()).isEqualTo(ReconciliationStrategy.MERGE_BY_ID);
		assertThat(relationshipDefinition.patchInputResolver().resolveSatelliteMutationIntents(
				new ReferencedManyPatch(Optional.of(List.of(1L, 2L)), List.of(3L))))
				.hasSize(3)
				.satisfies(intents ->
				{
					var intentList = List.copyOf(intents);
					assertThat(intentList.get(0)).isInstanceOf(
							SatelliteMutationIntent.ReferenceSatelliteMutationIntent.class);
					assertThat(intentList.get(1)).isInstanceOf(
							SatelliteMutationIntent.ReferenceSatelliteMutationIntent.class);
					assertThat(intentList.get(2)).isInstanceOf(
							SatelliteMutationIntent.RemoveSatelliteMutationIntent.class);
				});
	}

	@Test
	void ownedOneUsesUpsertCurrentConvention()
	{
		var relationshipDefinition = ownedOneRelationshipDefinition();

		assertThat(relationshipDefinition.cardinality()).isEqualTo(Cardinality.ONE);
		assertThat(relationshipDefinition.reconciliationStrategy()).isEqualTo(ReconciliationStrategy.REPLACE);
		assertThat(relationshipDefinition.createInputResolver().resolveSatelliteCreateIntent(
				new OwnedOneCreate(Optional.of(new SatelliteCreate("created")))))
				.singleElement()
				.isInstanceOfSatisfying(
						SatelliteCreateIntent.InlineSatelliteCreateIntent.class,
						intent -> assertThat(intent.satelliteDomainModelCreate()).isEqualTo(
								new SatelliteCreate("created")));
		assertThat(relationshipDefinition.patchInputResolver().resolveSatelliteMutationIntents(
				new OwnedOnePatch(Optional.of(new SatellitePatch(Optional.of("patched"))), List.of())))
				.singleElement()
				.isInstanceOfSatisfying(
						SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent.class,
						intent ->
						{
							assertThat(intent.satelliteDomainModelCreate()).isEqualTo(new SatelliteCreate("patched"));
							assertThat(intent.satelliteDomainModelUpdatePatch()).isEqualTo(
									new SatellitePatch(Optional.of("patched")));
						});
	}

	@Test
	void ownedManyMergeUsesPatchItemsAndRemoveIds()
	{
		var relationshipDefinition = ownedManyRelationshipDefinition(ReconciliationStrategy.MERGE_BY_ID);

		assertThat(relationshipDefinition.cardinality()).isEqualTo(Cardinality.MANY);
		assertThat(relationshipDefinition.reconciliationStrategy()).isEqualTo(ReconciliationStrategy.MERGE_BY_ID);
		assertThat(relationshipDefinition.patchInputResolver().resolveSatelliteMutationIntents(
				new OwnedManyPatch(
						Optional.of(List.of(
								SatelliteUpdatePatchItem.of(Optional.of(7L),
										new SatellitePatch(Optional.of("updated"))),
								SatelliteUpdatePatchItem.of(Optional.empty(), new SatellitePatch(Optional.of("new"))))),
						List.of(9L))))
				.hasSize(3)
				.satisfies(intents ->
				{
					var intentList = List.copyOf(intents);
					assertThat(intentList.get(0)).isInstanceOf(
							SatelliteMutationIntent.UpdateSatelliteMutationIntent.class);
					assertThat(intentList.get(1)).isInstanceOf(
							SatelliteMutationIntent.CreateSatelliteMutationIntent.class);
					assertThat(intentList.get(2)).isInstanceOf(
							SatelliteMutationIntent.RemoveSatelliteMutationIntent.class);
				});
	}

	@Test
	void ownedManyReplacePreservesCurrentConvention()
	{
		var relationshipDefinition = ownedManyRelationshipDefinition(ReconciliationStrategy.REPLACE);

		assertThat(relationshipDefinition.reconciliationStrategy()).isEqualTo(ReconciliationStrategy.REPLACE);
		assertThat(relationshipDefinition.patchInputResolver().resolveSatelliteMutationIntents(
				new OwnedManyPatch(
						Optional.of(List.of(
								SatelliteUpdatePatchItem.of(Optional.of(1L),
										new SatellitePatch(Optional.of("updated"))))),
						List.of())))
				.singleElement()
				.isInstanceOf(SatelliteMutationIntent.UpdateSatelliteMutationIntent.class);
	}

	@Test
	void oneCardinalityRejectsPatchAndRemoveCombination()
	{
		assertThatThrownBy(() -> ownedOneRelationshipDefinition().patchInputResolver().resolveSatelliteMutationIntents(
				new OwnedOnePatch(Optional.of(new SatellitePatch(Optional.of("patched"))), List.of(1L))))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("cannot combine a nested patch item with remove ids");
	}

	@Test
	void currentBindingRequiresReplaceBeforeBuild()
	{
		assertThatThrownBy(() -> AggregateRelationshipDefinitions
				.<String, OneMasterModel, OwnedOneCreate, OwnedOnePatch, Long, SatelliteDomainModel, SatelliteCreate, SatellitePatch>fromRelationship(
						Relationship.owned("satellite", SatelliteBaseModel.class)
						            .satelliteApiIdType(Long.class)
						            .satelliteDomainIdType(Long.class)
						            .satellitePersistenceIdType(java.util.UUID.class)
						            .build(),
						satelliteDefinition())
				.current(OneMasterModel::satellite)
				.build())
				.isInstanceOf(NullPointerException.class)
				.hasMessage("replace");
	}

	private static AggregateRelationshipDefinition<String, ReferencedOneMasterModel, ReferencedOneCreate,
			ReferencedOnePatch, Long, SatelliteDomainModel, SatelliteCreate, SatellitePatch>
	referencedOneRelationshipDefinition()
	{
		return AggregateRelationshipDefinitions
				.<String, ReferencedOneMasterModel, ReferencedOneCreate, ReferencedOnePatch, Long,
						SatelliteDomainModel, SatelliteCreate, SatellitePatch>fromRelationship(
						Relationship.referenced("satellite", SatelliteBaseModel.class)
						            .satelliteApiIdType(Long.class)
						            .satelliteDomainIdType(Long.class)
						            .satellitePersistenceIdType(java.util.UUID.class)
						            .build(),
						satelliteDefinition())
				.current(ReferencedOneMasterModel::satellite)
				.replace(ReferencedOneMasterModel::withSatellite)
				.build();
	}

	private static AggregateRelationshipDefinition<String, ReferencedManyMasterModel, ReferencedManyCreate,
			ReferencedManyPatch, Long, SatelliteDomainModel, SatelliteCreate, SatellitePatch>
	referencedManyRelationshipDefinition(final ReconciliationStrategy reconciliationStrategy)
	{
		return AggregateRelationshipDefinitions
				.<String, ReferencedManyMasterModel, ReferencedManyCreate, ReferencedManyPatch, Long,
						SatelliteDomainModel, SatelliteCreate, SatellitePatch>fromRelationship(
						Relationship.referenced("satellites", SatelliteBaseModel.class)
						            .satelliteApiIdType(Long.class)
						            .satelliteDomainIdType(Long.class)
						            .satellitePersistenceIdType(java.util.UUID.class)
						            .reconciliationStrategy(reconciliationStrategy)
						            .build(),
						satelliteDefinition())
				.currentMany(ReferencedManyMasterModel::satellites)
				.replaceMany(ReferencedManyMasterModel::withSatellites)
				.build();
	}

	private static AggregateRelationshipDefinition<String, OneMasterModel, OwnedOneCreate, OwnedOnePatch, Long,
			SatelliteDomainModel, SatelliteCreate, SatellitePatch> ownedOneRelationshipDefinition()
	{
		return AggregateRelationshipDefinitions
				.<String, OneMasterModel, OwnedOneCreate, OwnedOnePatch, Long, SatelliteDomainModel, SatelliteCreate,
						SatellitePatch>fromRelationship(
						Relationship.owned("satellite", SatelliteBaseModel.class)
						            .satelliteApiIdType(Long.class)
						            .satelliteDomainIdType(Long.class)
						            .satellitePersistenceIdType(java.util.UUID.class)
						            .build(),
						satelliteDefinition())
				.current(OneMasterModel::satellite)
				.replace(OneMasterModel::withSatellite)
				.build();
	}

	private static AggregateRelationshipDefinition<String, ManyMasterModel, OwnedManyCreate, OwnedManyPatch, Long,
			SatelliteDomainModel, SatelliteCreate, SatellitePatch> ownedManyRelationshipDefinition(
			final ReconciliationStrategy reconciliationStrategy)
	{
		return AggregateRelationshipDefinitions
				.<String, ManyMasterModel, OwnedManyCreate, OwnedManyPatch, Long, SatelliteDomainModel,
						SatelliteCreate, SatellitePatch>fromRelationship(
						Relationship.owned("satellites", SatelliteBaseModel.class)
						            .satelliteApiIdType(Long.class)
						            .satelliteDomainIdType(Long.class)
						            .satellitePersistenceIdType(java.util.UUID.class)
						            .reconciliationStrategy(reconciliationStrategy)
						            .build(),
						satelliteDefinition())
				.currentMany(ManyMasterModel::satellites)
				.replaceMany(ManyMasterModel::withSatellites)
				.build();
	}

	private static AggregateCrudDefinition<Long, SatelliteDomainModel, SatelliteCreate, SatellitePatch, String>
	satelliteDefinition()
	{
		return AggregateCrudDefinitions
				.<Long, SatelliteDomainModel, SatelliteCreate, SatellitePatch, String>aggregateCrudDefinition()
				.mutationPort(new AggregateMutationPort<>()
				{
					@Override
					public IdentifiedModel<Long, SatelliteDomainModel> create(final SatelliteDomainModel domainModel)
					{
						return IdentifiedModel.of(1L, domainModel);
					}

					@Override
					public void put(final Long domainId, final SatelliteDomainModel domainModel)
					{
					}

					@Override
					public IdentifiedModel<Long, SatelliteDomainModel> update(
							final Long domainId,
							final SatelliteDomainModel domainModel)
					{
						return IdentifiedModel.of(domainId, domainModel);
					}

					@Override
					public void delete(final Long domainId)
					{
					}
				})
				.fetchPort(new AggregateFetchPort<>()
				{
					@Override
					public Optional<IdentifiedModel<Long, SatelliteDomainModel>> findById(final Long domainId)
					{
						return Optional.of(IdentifiedModel.of(domainId, new SatelliteDomainModel("value-" + domainId)));
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteDomainModel>> findByIds(final Set<Long> domainIds)
					{
						return domainIds.stream().map(this::findById).flatMap(Optional::stream).toList();
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteDomainModel>> findAll()
					{
						return List.of();
					}

					@Override
					public Slice<IdentifiedModel<Long, SatelliteDomainModel>> findAll(final Pageable pageable)
					{
						return new SliceImpl<>(List.of());
					}
				})
				.createBuilder(create -> new SatelliteDomainModel(create.value()))
				.patcher((_, patch) -> new SatelliteDomainModel(patch.value().orElseThrow()))
				.responseBuilder(SatelliteDomainModel::value)
				.insertionPolicy(_ ->
				{
				})
				.patchPolicy((_, _) ->
				{
				})
				.deletionPolicy(_ ->
				{
				})
				.securityPolicy(DomainSecurityPolicy.allowing())
				.duplicateDefinition((left, right) -> left.value().equals(right.value()))
				.build();
	}

	private interface SatelliteBaseModel
	{
	}

	private record SatelliteDomainModel(String value)
	{
	}

	private record SatelliteCreate(String value)
	{
		public static SatelliteCreate fromUpdatePatch(final SatellitePatch patch)
		{
			return new SatelliteCreate(patch.value().orElseThrow());
		}
	}

	private record SatellitePatch(Optional<String> value)
	{
	}

	private record ReferencedOneCreate(Optional<Long> satellite)
	{
	}

	private record ReferencedOnePatch(Optional<Long> satellite, Collection<Long> removeSatelliteIds)
	{
	}

	private record ReferencedOneMasterModel(Optional<IdentifiedModel<Long, SatelliteDomainModel>> satellite)
	{
		private ReferencedOneMasterModel withSatellite(
				final Optional<IdentifiedModel<Long, SatelliteDomainModel>> satellite)
		{
			return new ReferencedOneMasterModel(satellite);
		}
	}

	private record ReferencedManyCreate(Collection<Long> satellites)
	{
	}

	private record ReferencedManyPatch(Optional<Collection<Long>> satellites, Collection<Long> removeSatelliteIds)
	{
	}

	private record ReferencedManyMasterModel(Collection<IdentifiedModel<Long, SatelliteDomainModel>> satellites)
	{
		private ReferencedManyMasterModel withSatellites(
				final Collection<IdentifiedModel<Long, SatelliteDomainModel>> satellites)
		{
			return new ReferencedManyMasterModel(List.copyOf(satellites));
		}
	}

	private record OwnedOneCreate(Optional<SatelliteCreate> satellite)
	{
	}

	private record OwnedOnePatch(Optional<SatellitePatch> satellite, Collection<Long> removeSatelliteIds)
	{
	}

	private record OneMasterModel(Optional<IdentifiedModel<Long, SatelliteDomainModel>> satellite)
	{
		private OneMasterModel withSatellite(final Optional<IdentifiedModel<Long, SatelliteDomainModel>> satellite)
		{
			return new OneMasterModel(satellite);
		}
	}

	private record OwnedManyCreate(Collection<SatelliteCreate> satellites)
	{
	}

	private record OwnedManyPatch(
			Optional<Collection<SatelliteUpdatePatchItem<Long, SatellitePatch>>> satellites,
			Collection<Long> removeSatelliteIds)
	{
	}

	private record ManyMasterModel(Collection<IdentifiedModel<Long, SatelliteDomainModel>> satellites)
	{
		private ManyMasterModel withSatellites(final Collection<IdentifiedModel<Long, SatelliteDomainModel>> satellites)
		{
			return new ManyMasterModel(List.copyOf(satellites));
		}
	}
}