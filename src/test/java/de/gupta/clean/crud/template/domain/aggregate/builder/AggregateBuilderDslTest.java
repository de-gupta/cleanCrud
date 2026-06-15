package de.gupta.clean.crud.template.domain.aggregate.builder;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.aggregate.execution.DefaultAggregateLifecycleEngine;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.aggregate.relationship.standard.SatelliteUpdatePatchItem;
import de.gupta.clean.crud.template.domain.aggregate.relationship.standard.StandardSatelliteLifecycleSemantics;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("Aggregate builder DSL tests")
final class AggregateBuilderDslTest
{
	@org.junit.jupiter.api.Test
	void oneToOneStandardSatelliteBuilderUsesIdBackedDefaults()
	{
		var relationshipDefinition = new StandardRelationshipTestScenario().oneToOneRelationshipDefinition();

		assertThat(relationshipDefinition.lifecycleSemantics())
				.as("one-to-one standard relationships should use the id-backed lifecycle defaults")
				.isEqualTo(StandardSatelliteLifecycleSemantics.idBackedDefaults());
		assertThat(relationshipDefinition.reconciliationStrategy())
				.as("one-to-one standard relationships should default to REPLACE")
				.isEqualTo(ReconciliationStrategy.REPLACE);
	}

	@org.junit.jupiter.api.Test
	void oneToManyStandardSatelliteBuilderUsesMergeByIdByDefault()
	{
		var relationshipDefinition = new StandardRelationshipTestScenario().oneToManyRelationshipDefinition();

		assertThat(relationshipDefinition.lifecycleSemantics())
				.as("one-to-many standard relationships should use the id-backed lifecycle defaults")
				.isEqualTo(StandardSatelliteLifecycleSemantics.idBackedDefaults());
		assertThat(relationshipDefinition.reconciliationStrategy())
				.as("one-to-many standard relationships should default to MERGE_BY_ID")
				.isEqualTo(ReconciliationStrategy.MERGE_BY_ID);
	}

	@org.junit.jupiter.api.Test
	void oneToOneStandardSatelliteBuilderHydratesAndUpsertsCurrentSatellite()
	{
		var scenario = new StandardRelationshipTestScenario();
		scenario.installOneToOneRelationship();

		var saved = scenario.saveService().save(new StandardMasterCreate(
				"master",
				Optional.of(new StandardSatelliteCreate("version-one")),
				List.of()));
		assertThat(saved.model().versions())
				.as("save should hydrate the linked one-to-one satellite as the public contract")
				.extracting(StandardSatellitePublicResponse::value)
				.containsExactly("version-one");

		var updated = scenario.updateService().updateById(
				saved.id(),
				new StandardMasterPatch(
						Optional.of(SatelliteUpdatePatchItem.of(
								Optional.empty(),
								new StandardSatelliteUpdatePatch(Optional.of("version-two")))),
						List.of(),
						List.of(),
						List.of()));
		assertThat(updated.model().versions())
				.as("an id-less one-to-one patch item should upsert the current linked satellite")
				.extracting(StandardSatellitePublicResponse::value)
				.containsExactly("version-two");

		var fetched = scenario.fetchService().findById(saved.id());
		assertThat(fetched.model().versions())
				.as("fetch should hydrate the updated one-to-one satellite using the default hydration strategy")
				.extracting(StandardSatellitePublicResponse::value)
				.containsExactly("version-two");
	}

	@org.junit.jupiter.api.Test
	void oneToManyStandardSatelliteBuilderCreatesUpdatesAndRemovesById()
	{
		var scenario = new StandardRelationshipTestScenario();
		scenario.installOneToManyRelationship();

		var saved = scenario.saveService().save(new StandardMasterCreate(
				"master",
				Optional.empty(),
				List.of(
						new StandardSatelliteCreate("note-one"),
						new StandardSatelliteCreate("note-two"))));
		assertThat(saved.model().notes())
				.as("save should hydrate all linked one-to-many satellites as public contracts")
				.extracting(StandardSatellitePublicResponse::value)
				.containsExactly("note-one", "note-two");

		var noteToUpdate = saved.model().notes().getFirst().id();
		var noteToRemove = saved.model().notes().get(1).id();
		var updated = scenario.updateService().updateById(
				saved.id(),
				new StandardMasterPatch(
						Optional.empty(),
						List.of(),
						List.of(
								SatelliteUpdatePatchItem.of(
										Optional.of(noteToUpdate),
										new StandardSatelliteUpdatePatch(Optional.of("note-one-updated"))),
								SatelliteUpdatePatchItem.of(
										Optional.empty(),
										new StandardSatelliteUpdatePatch(Optional.of("note-three")))),
						List.of(noteToRemove)));

		assertThat(updated.model().notes())
				.as("many standard relationships should update by id, create id-less items, and remove explicit ids")
				.extracting(StandardSatellitePublicResponse::value)
				.containsExactly("note-one-updated", "note-three");
	}

	@org.junit.jupiter.api.Test
	void oneToOneReferencedSatelliteBuilderLinksExistingSatelliteAndHydratesIt()
	{
		var scenario = new StandardRelationshipTestScenario();
		scenario.installOneToOneReferencedRelationship();
		var versionId = scenario.createStandaloneSatellite("version-one");

		var saved = scenario.referencedSaveService().save(new StandardReferenceMasterCreate(
				"master",
				Optional.of(versionId),
				List.of()));

		assertThat(saved.model().versions())
				.as("reference-only one-to-one relationships should link and hydrate an existing satellite")
				.extracting(StandardSatellitePublicResponse::id, StandardSatellitePublicResponse::value)
				.containsExactly(tuple(versionId, "version-one"));
	}

	@org.junit.jupiter.api.Test
	void oneToOneReferencedSatelliteBuilderThrowsWhenReferencedSatelliteDoesNotExist()
	{
		var scenario = new StandardRelationshipTestScenario();
		scenario.installOneToOneReferencedRelationship();

		assertThatThrownBy(() -> scenario.referencedSaveService().save(new StandardReferenceMasterCreate(
				"master",
				Optional.of(999L),
				List.of())))
				.as("reference-only one-to-one relationships should validate referenced ids")
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@org.junit.jupiter.api.Test
	void oneToManyReferencedSatelliteBuilderAddsAndRemovesReferencesById()
	{
		var scenario = new StandardRelationshipTestScenario();
		scenario.installOneToManyReferencedRelationship();
		var firstNoteId = scenario.createStandaloneSatellite("note-one");
		var secondNoteId = scenario.createStandaloneSatellite("note-two");
		var thirdNoteId = scenario.createStandaloneSatellite("note-three");

		var saved = scenario.referencedSaveService().save(new StandardReferenceMasterCreate(
				"master",
				Optional.empty(),
				List.of(firstNoteId, secondNoteId)));

		var updated = scenario.referencedUpdateService().updateById(
				saved.id(),
				new StandardReferenceMasterPatch(
						Optional.empty(),
						List.of(),
						List.of(thirdNoteId),
						List.of(secondNoteId)));

		assertThat(updated.model().notes())
				.as("reference-only one-to-many relationships should add and remove linked ids without creating satellites")
				.extracting(StandardSatellitePublicResponse::id, StandardSatellitePublicResponse::value)
				.containsExactly(
						tuple(firstNoteId, "note-one"),
						tuple(thirdNoteId, "note-three"));
	}

	@org.junit.jupiter.api.Test
	void relationshipDefaultsToNoOpHydrationStrategyWhenHydrationOnFetchIsDisabled()
	{
		var scenario = new TestScenario();
		var relationshipDefinition = AggregateRelationshipDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>aggregateRelationshipDefinition()
				.name("satellite")
				.cardinality(Cardinality.ONE)
				.lifecycleSemantics(LifecycleSemantics.none())
				.satelliteDefinition(scenario.satelliteDefinition())
				.createInputResolver(MasterCreate::satelliteCreateIntents)
				.patchInputResolver(MasterPatch::satelliteMutationIntents)
				.identityResolver((_, _) -> Optional.empty())
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT))
				.build();

		var hydrated = relationshipDefinition.hydrationStrategy().hydrate(
				IdentifiedModel.of("master-1", new MasterModel("master", List.of(1L), List.of())),
				new AggregateFetchPort<>()
				{
					@Override
					public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long domainId)
					{
						throw new AssertionError("No-op hydration strategy should not fetch satellites");
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> domainIds)
					{
						return List.of();
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
					{
						return List.of();
					}

					@Override
					public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
					{
						return new SliceImpl<>(List.of());
					}
				},
				scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT));

		assertThat(hydrated)
				.as("No-op hydration should return the unchanged master model")
				.isEqualTo(new MasterModel("master", List.of(1L), List.of()));
	}

	@org.junit.jupiter.api.Test
	void relationshipStillRequiresHydrationStrategyWhenHydrationOnFetchIsEnabled()
	{
		var scenario = new TestScenario();
		assertThatThrownBy(() -> AggregateRelationshipDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>aggregateRelationshipDefinition()
				.name("satellite")
				.cardinality(Cardinality.ONE)
				.lifecycleSemantics(LifeCycleBuilders.oneToOneLifecycle())
				.satelliteDefinition(scenario.satelliteDefinition())
				.createInputResolver(MasterCreate::satelliteCreateIntents)
				.patchInputResolver(MasterPatch::satelliteMutationIntents)
				.identityResolver((_, _) -> Optional.empty())
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT))
				.build())
				.as("Hydrating relationships should still require an explicit hydration strategy")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("hydrationStrategy");
	}

	@org.junit.jupiter.api.Test
	void aggregateBuilderDefaultsPostCommitMutationToNoOpAndAllowsOverride()
	{
		var scenario = new TestScenario();
		PostCommitMutation<String, MasterModel> configuredMutation = _ ->
		{
		};

		var defaultDefinition = AggregateDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>aggregateCrudDefinition()
				.mutationPort(scenario.masterMutationPort())
				.fetchPort(scenario.masterFetchPort())
				.createBuilder(create -> new MasterModel(create.value(), List.of(), List.of()))
				.patcher((originalDomainModel, patch) -> new MasterModel(
						patch.value() == null ? originalDomainModel.value() : patch.value(),
						originalDomainModel.satelliteDomainIds(),
						originalDomainModel.hydratedSatellites()))
				.responseBuilder(MasterResponse::from)
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
				.duplicateDefinition((KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value)
				.build();
		var overriddenDefinition = AggregateDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>aggregateCrudDefinition()
				.mutationPort(scenario.masterMutationPort())
				.fetchPort(scenario.masterFetchPort())
				.createBuilder(create -> new MasterModel(create.value(), List.of(), List.of()))
				.patcher((originalDomainModel, patch) -> new MasterModel(
						patch.value() == null ? originalDomainModel.value() : patch.value(),
						originalDomainModel.satelliteDomainIds(),
						originalDomainModel.hydratedSatellites()))
				.responseBuilder(MasterResponse::from)
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
				.duplicateDefinition((KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value)
				.postCommitMutation(configuredMutation)
				.build();

		defaultDefinition.postCommitMutation().accept(new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				"id",
				Optional.of(new MasterModel("value", List.of(), List.of())),
				Optional.empty()));
		assertThat(overriddenDefinition.postCommitMutation())
				.as("builder should preserve an explicitly configured post-commit mutation")
				.isSameAs(configuredMutation);
	}

	private record MasterCreate(
			String value,
			Collection<SatelliteCreateIntent<Long, SatelliteCreate>> satelliteCreateIntents)
	{
	}

	private record MasterPatch(
			String value,
			Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>> satelliteMutationIntents)
	{
	}

	private record MasterModel(
			String value,
			List<Long> satelliteDomainIds,
			List<SatelliteModel> hydratedSatellites)
	{
		private MasterModel withSatelliteDomainIds(final Collection<Long> newSatelliteDomainIds)
		{
			return new MasterModel(value, List.copyOf(newSatelliteDomainIds), hydratedSatellites);
		}

		private MasterModel withHydratedSatellites(final Collection<IdentifiedModel<Long, SatelliteModel>> satellites)
		{
			return new MasterModel(value, satelliteDomainIds, satellites.stream().map(IdentifiedModel::model).toList());
		}
	}

	private record MasterResponse(String value, List<Long> satelliteDomainIds, List<String> hydratedSatelliteValues)
	{
		private static MasterResponse from(final MasterModel masterDomainModel)
		{
			return new MasterResponse(masterDomainModel.value(), masterDomainModel.satelliteDomainIds(),
					masterDomainModel.hydratedSatellites().stream().map(SatelliteModel::value).toList());
		}
	}

	private record SatelliteCreate(String value)
	{
	}

	private record SatellitePatch(String value)
	{
	}

	private record SatelliteModel(String value)
	{
	}

	private record StandardMasterCreate(
			String value,
			Optional<StandardSatelliteCreate> version,
			Collection<StandardSatelliteCreate> notes)
	{
	}

	private record StandardMasterPatch(
			Optional<SatelliteUpdatePatchItem<Long, StandardSatelliteUpdatePatch>> version,
			Collection<Long> removeVersionIds,
			Collection<SatelliteUpdatePatchItem<Long, StandardSatelliteUpdatePatch>> notes,
			Collection<Long> removeNoteIds)
	{
	}

	private record StandardMasterModel(
			String value,
			List<StandardSatellitePublicResponse> versions,
			List<StandardSatellitePublicResponse> notes)
	{
		private StandardMasterModel withVersions(final Collection<StandardSatellitePublicResponse> versions)
		{
			return new StandardMasterModel(value, List.copyOf(versions), notes);
		}

		private StandardMasterModel withNotes(final Collection<StandardSatellitePublicResponse> notes)
		{
			return new StandardMasterModel(value, versions, List.copyOf(notes));
		}
	}

	private record StandardMasterResponse(
			String value,
			List<StandardSatellitePublicResponse> versions,
			List<StandardSatellitePublicResponse> notes)
	{
		private static StandardMasterResponse from(final StandardMasterModel masterDomainModel)
		{
			return new StandardMasterResponse(masterDomainModel.value(), masterDomainModel.versions(),
					masterDomainModel.notes());
		}
	}

	private record StandardReferenceMasterCreate(
			String value,
			Optional<Long> versionId,
			Collection<Long> noteIds)
	{
	}

	private record StandardReferenceMasterPatch(
			Optional<Long> versionId,
			Collection<Long> removeVersionIds,
			Collection<Long> noteIds,
			Collection<Long> removeNoteIds)
	{
	}

	private record StandardSatelliteCreate(String value)
	{
	}

	private record StandardSatelliteUpdatePatch(Optional<String> value)
	{
	}

	private record StandardSatelliteDomainCreate(String value)
	{
	}

	private record StandardSatelliteDomainPatch(String value)
	{
	}

	private record StandardSatelliteDomainModel(String value)
	{
	}

	private record StandardSatelliteAggregateResponse(String value)
	{
	}

	private record StandardSatellitePublicResponse(Long id, String value)
	{
	}

	private static final class TestScenario
	{
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		private final AtomicInteger generatedMasterIds = new AtomicInteger();
		private final AtomicInteger generatedSatelliteIds = new AtomicInteger();
		private final AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
				satelliteDefinition = satelliteDefinition();
		private AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
				masterDefinition =
				masterDefinition(List.of());

		private void installRelationship(
				final Cardinality cardinality,
				final ReconciliationStrategy reconciliationStrategy,
				final LifecycleSemantics lifecycleSemantics,
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			var relationshipDefinition = AggregateRelationshipDefinitions
					.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>
							aggregateRelationshipDefinition()
					.name("satellite")
					.cardinality(cardinality)
					.lifecycleSemantics(lifecycleSemantics)
					.satelliteDefinition(satelliteDefinition)
					.createInputResolver(MasterCreate::satelliteCreateIntents)
					.patchInputResolver(MasterPatch::satelliteMutationIntents)
					.identityResolver((_, _) -> Optional.empty())
					.reconciliationStrategy(reconciliationStrategy)
					.linkStrategy(linkStrategy(satellitePersistenceOrder))
					.hydrationStrategy((master, satelliteFetchPort, satelliteLinkStrategy) ->
					{
						var hydratedSatellites = new ArrayList<IdentifiedModel<Long, SatelliteModel>>();
						for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(
								master.model()))
						{
							satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
						}
						return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
					})
					.build();
			masterDefinition = masterDefinition(List.of(relationshipDefinition));
		}

		private SatelliteLinkStrategy<String, MasterModel, Long, SatelliteModel> linkStrategy(
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			return new SatelliteLinkStrategy<>()
			{
				@Override
				public SatellitePersistenceOrder persistenceOrder()
				{
					return satellitePersistenceOrder;
				}

				@Override
				public Optional<Long> currentLinkedSatelliteDomainId(final MasterModel masterDomainModel)
				{
					return masterDomainModel.satelliteDomainIds().stream().findFirst();
				}

				@Override
				public Collection<Long> currentLinkedSatelliteDomainIds(final MasterModel masterDomainModel)
				{
					return masterDomainModel.satelliteDomainIds();
				}

				@Override
				public MasterModel replaceLinkedSatelliteDomainIds(
						final MasterModel masterDomainModel,
						final Collection<Long> satelliteDomainIds)
				{
					return masterDomainModel.withSatelliteDomainIds(satelliteDomainIds);
				}

				@Override
				public MasterModel attachHydratedSatellites(
						final MasterModel masterDomainModel,
						final Collection<IdentifiedModel<Long, SatelliteModel>> satellites)
				{
					return masterDomainModel.withHydratedSatellites(satellites);
				}
			};
		}

		private AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> masterDefinition(
				final Collection<? extends AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
						relationships)
		{
			return AggregateDefinitions
					.<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>aggregateCrudDefinition()
					.mutationPort(masterMutationPort())
					.fetchPort(masterFetchPort())
					.createBuilder(create -> new MasterModel(create.value(), List.of(), List.of()))
					.patcher((originalDomainModel, patch) -> new MasterModel(
							patch.value() == null ? originalDomainModel.value() : patch.value(),
							originalDomainModel.satelliteDomainIds(),
							originalDomainModel.hydratedSatellites()))
					.responseBuilder(MasterResponse::from)
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
					.duplicateDefinition((KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value)
					.relationshipDefinitions(relationships)
					.build();
		}

		private AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> masterMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					var masterDomainId = "master-" + generatedMasterIds.incrementAndGet();
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void put(final String masterDomainId, final MasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String masterDomainId,
				                                                   final MasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void delete(final String masterDomainId)
				{
					masterStore.remove(masterDomainId);
				}
			};
		}

		private AggregateFetchPort<String, MasterModel> masterFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, MasterModel>> findById(final String masterDomainId)
				{
					return Optional.ofNullable(masterStore.get(masterDomainId))
					               .map(model -> IdentifiedModel.of(masterDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findByIds(final Set<String> masterDomainIds)
				{
					return masterDomainIds.stream().flatMap(masterDomainId -> findById(masterDomainId).stream())
					                      .toList();
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findAll()
				{
					return masterStore.entrySet().stream()
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<String, MasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		private TestSaveService saveService()
		{
			return new TestSaveService(masterDefinition, engine);
		}

		private TestUpdateService updateService()
		{
			return new TestUpdateService(masterDefinition, engine);
		}

		private TestFetchService fetchService()
		{
			return new TestFetchService(masterDefinition, engine);
		}

		private AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String> satelliteDefinition()
		{
			return AggregateDefinitions
					.<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>aggregateCrudDefinition()
					.mutationPort(satelliteMutationPort())
					.fetchPort(satelliteFetchPort())
					.createBuilder(create -> new SatelliteModel(create.value()))
					.patcher((_, patch) -> new SatelliteModel(patch.value()))
					.responseBuilder(SatelliteModel::value)
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

		private AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> satelliteMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
				{
					generatedSatelliteIds.updateAndGet(current -> Math.max(current, satelliteStore.keySet().stream()
					                                                                              .mapToInt(
																										  Long::intValue)
					                                                                              .max()
					                                                                              .orElse(0)));
					var satelliteDomainId = (long) generatedSatelliteIds.incrementAndGet();
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void put(final Long satelliteDomainId, final SatelliteModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, SatelliteModel> update(final Long satelliteDomainId,
				                                                    final SatelliteModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void delete(final Long satelliteDomainId)
				{
					satelliteStore.remove(satelliteDomainId);
				}
			};
		}

		private AggregateFetchPort<Long, SatelliteModel> satelliteFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long satelliteDomainId)
				{
					return Optional.ofNullable(satelliteStore.get(satelliteDomainId))
					               .map(model -> IdentifiedModel.of(satelliteDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> satelliteDomainIds)
				{
					return satelliteDomainIds.stream()
					                         .flatMap(satelliteDomainId -> findById(satelliteDomainId).stream())
					                         .toList();
				}

				@Override
				public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
				{
					return satelliteStore.entrySet().stream()
					                     .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}
	}

	private static final class StandardRelationshipTestScenario
	{
		private final Map<String, StandardMasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, StandardSatelliteDomainModel> satelliteStore = new LinkedHashMap<>();
		private final AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		private final AtomicInteger generatedMasterIds = new AtomicInteger();
		private final AtomicInteger generatedSatelliteIds = new AtomicInteger();
		private final AggregateDefinition<Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate,
				StandardSatelliteDomainPatch, StandardSatelliteAggregateResponse> satelliteDefinition =
				satelliteDefinition();
		private AggregateDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
				StandardMasterResponse> masterDefinition =
				masterDefinition(List.of());
		private AggregateDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
				StandardReferenceMasterPatch, StandardMasterResponse> referencedMasterDefinition =
				referencedMasterDefinition(List.of());

		private void installOneToOneRelationship()
		{
			masterDefinition = masterDefinition(List.of(oneToOneRelationshipDefinition()));
		}

		private AggregateDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
				StandardMasterResponse> masterDefinition(
				final Collection<? extends AggregateRelationshipDefinitionContract<String, StandardMasterModel,
						StandardMasterCreate, StandardMasterPatch>> relationships)
		{
			return AggregateDefinitions
					.<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
							StandardMasterResponse>aggregateCrudDefinition()
					.mutationPort(masterMutationPort())
					.fetchPort(masterFetchPort())
					.createBuilder(create -> new StandardMasterModel(create.value(), List.of(), List.of()))
					.patcher((originalDomainModel, _) -> originalDomainModel)
					.responseBuilder(StandardMasterResponse::from)
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
					.duplicateDefinition(
							(KeyBasedDuplicateDefinition<StandardMasterModel, String>) StandardMasterModel::value)
					.relationshipDefinitions(relationships)
					.build();
		}

		private AggregateRelationshipDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
				Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch>
		oneToOneRelationshipDefinition()
		{
			return AggregateRelationshipDefinitions
					.<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch, Long,
							StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch,
							StandardSatelliteAggregateResponse, StandardSatelliteCreate, StandardSatelliteUpdatePatch,
							StandardSatellitePublicResponse>oneToOneSatellite("version", satelliteDefinition)
					.createExtractor(StandardMasterCreate::version)
					.createMapper(create -> new StandardSatelliteDomainCreate(create.value()))
					.patchExtractor(StandardMasterPatch::version)
					.patchMapper(patch -> new StandardSatelliteDomainPatch(patch.value().orElseThrow()))
					.patchCreateMapper(patch -> new StandardSatelliteDomainCreate(patch.value().orElseThrow()))
					.currentSatellite(masterDomainModel -> masterDomainModel.versions().stream().findFirst())
					.replaceSatellite((masterDomainModel, version) -> masterDomainModel.withVersions(
							version.stream().toList()))
					.publicResponseMapper(identifiedAggregateResponse -> new StandardSatellitePublicResponse(
							identifiedAggregateResponse.id(),
							identifiedAggregateResponse.model().value()))
					.build();
		}

		private AggregateMutationPort<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch>
		masterMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, StandardMasterModel> create(final StandardMasterModel domainModel)
				{
					var masterDomainId = "master-" + generatedMasterIds.incrementAndGet();
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void put(final String masterDomainId, final StandardMasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, StandardMasterModel> update(
						final String masterDomainId,
						final StandardMasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void delete(final String masterDomainId)
				{
					masterStore.remove(masterDomainId);
				}
			};
		}

		private AggregateFetchPort<String, StandardMasterModel> masterFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, StandardMasterModel>> findById(final String masterDomainId)
				{
					return Optional.ofNullable(masterStore.get(masterDomainId))
					               .map(model -> IdentifiedModel.of(masterDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, StandardMasterModel>> findByIds(
						final Set<String> masterDomainIds)
				{
					return masterDomainIds.stream().flatMap(masterDomainId -> findById(masterDomainId).stream())
					                      .toList();
				}

				@Override
				public Collection<IdentifiedModel<String, StandardMasterModel>> findAll()
				{
					return masterStore.entrySet().stream()
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<String, StandardMasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		private void installOneToManyRelationship()
		{
			masterDefinition = masterDefinition(List.of(oneToManyRelationshipDefinition()));
		}

		private AggregateRelationshipDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
				Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch>
		oneToManyRelationshipDefinition()
		{
			return AggregateRelationshipDefinitions
					.<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch, Long,
							StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch,
							StandardSatelliteAggregateResponse, StandardSatelliteCreate, StandardSatelliteUpdatePatch,
							StandardSatellitePublicResponse>oneToManySatellite("note", satelliteDefinition)
					.createExtractor(StandardMasterCreate::notes)
					.createMapper(create -> new StandardSatelliteDomainCreate(create.value()))
					.patchExtractor(StandardMasterPatch::notes)
					.patchMapper(patch -> new StandardSatelliteDomainPatch(patch.value().orElseThrow()))
					.patchCreateMapper(patch -> new StandardSatelliteDomainCreate(patch.value().orElseThrow()))
					.removeIdExtractor(StandardMasterPatch::removeNoteIds)
					.currentSatellites(StandardMasterModel::notes)
					.replaceSatellites(StandardMasterModel::withNotes)
					.publicResponseMapper(identifiedAggregateResponse -> new StandardSatellitePublicResponse(
							identifiedAggregateResponse.id(),
							identifiedAggregateResponse.model().value()))
					.build();
		}

		private void installOneToOneReferencedRelationship()
		{
			referencedMasterDefinition =
					referencedMasterDefinition(List.of(oneToOneReferencedRelationshipDefinition()));
		}

		private AggregateDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
				StandardReferenceMasterPatch, StandardMasterResponse> referencedMasterDefinition(
				final Collection<? extends AggregateRelationshipDefinitionContract<String, StandardMasterModel,
						StandardReferenceMasterCreate, StandardReferenceMasterPatch>> relationships)
		{
			return AggregateDefinitions
					.<String, StandardMasterModel, StandardReferenceMasterCreate, StandardReferenceMasterPatch,
							StandardMasterResponse>aggregateCrudDefinition()
					.mutationPort(referencedMasterMutationPort())
					.fetchPort(referencedMasterFetchPort())
					.createBuilder(create -> new StandardMasterModel(create.value(), List.of(), List.of()))
					.patcher((originalDomainModel, _) -> originalDomainModel)
					.responseBuilder(StandardMasterResponse::from)
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
					.duplicateDefinition(
							(KeyBasedDuplicateDefinition<StandardMasterModel, String>) StandardMasterModel::value)
					.relationshipDefinitions(relationships)
					.build();
		}

		private AggregateRelationshipDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
				StandardReferenceMasterPatch, Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate,
				StandardSatelliteDomainPatch> oneToOneReferencedRelationshipDefinition()
		{
			return AggregateRelationshipDefinitions
					.<String, StandardMasterModel, StandardReferenceMasterCreate, StandardReferenceMasterPatch, Long,
							StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch,
							StandardSatelliteAggregateResponse, StandardSatellitePublicResponse>
							oneToOneReferencedSatellite("version", satelliteDefinition)
					.createReferenceIdExtractor(StandardReferenceMasterCreate::versionId)
					.patchReferenceIdExtractor(StandardReferenceMasterPatch::versionId)
					.removeIdExtractor(StandardReferenceMasterPatch::removeVersionIds)
					.currentSatellite(masterDomainModel -> masterDomainModel.versions().stream().findFirst())
					.replaceSatellite((masterDomainModel, version) -> masterDomainModel.withVersions(
							version.stream().toList()))
					.publicResponseMapper(identifiedAggregateResponse -> new StandardSatellitePublicResponse(
							identifiedAggregateResponse.id(),
							identifiedAggregateResponse.model().value()))
					.build();
		}

		private AggregateMutationPort<String, StandardMasterModel, StandardReferenceMasterCreate,
				StandardReferenceMasterPatch> referencedMasterMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, StandardMasterModel> create(final StandardMasterModel domainModel)
				{
					var masterDomainId = "master-" + generatedMasterIds.incrementAndGet();
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void put(final String masterDomainId, final StandardMasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, StandardMasterModel> update(
						final String masterDomainId,
						final StandardMasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void delete(final String masterDomainId)
				{
					masterStore.remove(masterDomainId);
				}
			};
		}

		private AggregateFetchPort<String, StandardMasterModel> referencedMasterFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, StandardMasterModel>> findById(final String masterDomainId)
				{
					return Optional.ofNullable(masterStore.get(masterDomainId))
					               .map(model -> IdentifiedModel.of(masterDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, StandardMasterModel>> findByIds(
						final Set<String> masterDomainIds)
				{
					return masterDomainIds.stream().flatMap(masterDomainId -> findById(masterDomainId).stream())
					                      .toList();
				}

				@Override
				public Collection<IdentifiedModel<String, StandardMasterModel>> findAll()
				{
					return masterStore.entrySet().stream()
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<String, StandardMasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		private void installOneToManyReferencedRelationship()
		{
			referencedMasterDefinition =
					referencedMasterDefinition(List.of(oneToManyReferencedRelationshipDefinition()));
		}

		private AggregateRelationshipDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
				StandardReferenceMasterPatch, Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate,
				StandardSatelliteDomainPatch> oneToManyReferencedRelationshipDefinition()
		{
			return AggregateRelationshipDefinitions
					.<String, StandardMasterModel, StandardReferenceMasterCreate, StandardReferenceMasterPatch, Long,
							StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch,
							StandardSatelliteAggregateResponse, StandardSatellitePublicResponse>
							oneToManyReferencedSatellite("note", satelliteDefinition)
					.createReferenceIdsExtractor(StandardReferenceMasterCreate::noteIds)
					.patchReferenceIdsExtractor(StandardReferenceMasterPatch::noteIds)
					.removeIdExtractor(StandardReferenceMasterPatch::removeNoteIds)
					.currentSatellites(StandardMasterModel::notes)
					.replaceSatellites(StandardMasterModel::withNotes)
					.publicResponseMapper(identifiedAggregateResponse -> new StandardSatellitePublicResponse(
							identifiedAggregateResponse.id(),
							identifiedAggregateResponse.model().value()))
					.build();
		}

		private TestStandardSaveService saveService()
		{
			return new TestStandardSaveService(masterDefinition, engine);
		}

		private TestStandardUpdateService updateService()
		{
			return new TestStandardUpdateService(masterDefinition, engine);
		}

		private TestStandardFetchService fetchService()
		{
			return new TestStandardFetchService(masterDefinition, engine);
		}

		private TestStandardReferencedSaveService referencedSaveService()
		{
			return new TestStandardReferencedSaveService(referencedMasterDefinition, engine);
		}

		private TestStandardReferencedUpdateService referencedUpdateService()
		{
			return new TestStandardReferencedUpdateService(referencedMasterDefinition, engine);
		}

		private long createStandaloneSatellite(final String value)
		{
			return satelliteMutationPort().create(new StandardSatelliteDomainModel(value)).id();
		}

		private AggregateMutationPort<Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate,
				StandardSatelliteDomainPatch> satelliteMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<Long, StandardSatelliteDomainModel> create(
						final StandardSatelliteDomainModel domainModel)
				{
					generatedSatelliteIds.updateAndGet(current -> Math.max(current, satelliteStore.keySet().stream()
					                                                                              .mapToInt(
																										  Long::intValue)
					                                                                              .max()
					                                                                              .orElse(0)));
					var satelliteDomainId = (long) generatedSatelliteIds.incrementAndGet();
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void put(final Long satelliteDomainId, final StandardSatelliteDomainModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, StandardSatelliteDomainModel> update(
						final Long satelliteDomainId,
						final StandardSatelliteDomainModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void delete(final Long satelliteDomainId)
				{
					satelliteStore.remove(satelliteDomainId);
				}
			};
		}

		private AggregateDefinition<Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate,
				StandardSatelliteDomainPatch, StandardSatelliteAggregateResponse> satelliteDefinition()
		{
			return AggregateDefinitions
					.<Long, StandardSatelliteDomainModel, StandardSatelliteDomainCreate, StandardSatelliteDomainPatch,
							StandardSatelliteAggregateResponse>aggregateCrudDefinition()
					.mutationPort(satelliteMutationPort())
					.fetchPort(satelliteFetchPort())
					.createBuilder(create -> new StandardSatelliteDomainModel(create.value()))
					.patcher((_, patch) -> new StandardSatelliteDomainModel(patch.value()))
					.responseBuilder(satelliteDomainModel -> new StandardSatelliteAggregateResponse(
							satelliteDomainModel.value()))
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

		private AggregateFetchPort<Long, StandardSatelliteDomainModel> satelliteFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<Long, StandardSatelliteDomainModel>> findById(
						final Long satelliteDomainId)
				{
					return Optional.ofNullable(satelliteStore.get(satelliteDomainId))
					               .map(model -> IdentifiedModel.of(satelliteDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<Long, StandardSatelliteDomainModel>> findByIds(
						final Set<Long> satelliteDomainIds)
				{
					return satelliteDomainIds.stream()
					                         .flatMap(satelliteDomainId -> findById(satelliteDomainId).stream())
					                         .toList();
				}

				@Override
				public Collection<IdentifiedModel<Long, StandardSatelliteDomainModel>> findAll()
				{
					return satelliteStore.entrySet().stream()
					                     .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<Long, StandardSatelliteDomainModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}
	}

	private static final class TestStandardSaveService
			extends AbstractSaveService<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
			StandardMasterResponse>
	{
		private TestStandardSaveService(
				final AggregateDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
						StandardMasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.saveCoordinator());
		}
	}

	private static final class TestStandardUpdateService
			extends AbstractUpdateService<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
			StandardMasterResponse>
	{
		private TestStandardUpdateService(
				final AggregateDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
						StandardMasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.updateCoordinator());
		}
	}

	private static final class TestStandardFetchService
			extends AbstractFetchService<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
			StandardMasterResponse>
	{
		private TestStandardFetchService(
				final AggregateDefinition<String, StandardMasterModel, StandardMasterCreate, StandardMasterPatch,
						StandardMasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.fetchCoordinator());
		}
	}

	private static final class TestStandardReferencedSaveService
			extends AbstractSaveService<String, StandardMasterModel, StandardReferenceMasterCreate,
			StandardReferenceMasterPatch, StandardMasterResponse>
	{
		private TestStandardReferencedSaveService(
				final AggregateDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
						StandardReferenceMasterPatch, StandardMasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.saveCoordinator());
		}
	}

	private static final class TestStandardReferencedUpdateService
			extends AbstractUpdateService<String, StandardMasterModel, StandardReferenceMasterCreate,
			StandardReferenceMasterPatch, StandardMasterResponse>
	{
		private TestStandardReferencedUpdateService(
				final AggregateDefinition<String, StandardMasterModel, StandardReferenceMasterCreate,
						StandardReferenceMasterPatch, StandardMasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.updateCoordinator());
		}
	}

	private static final class TestSaveService
			extends AbstractSaveService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestSaveService(
				final AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.saveCoordinator());
		}
	}

	private static final class TestUpdateService
			extends AbstractUpdateService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestUpdateService(
				final AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.updateCoordinator());
		}
	}

	private static final class TestFetchService
			extends AbstractFetchService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestFetchService(
				final AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.fetchCoordinator());
		}
	}

	private static final class InlineTransactionRunner implements PersistenceTransactionRunner
	{
		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			return action.get();
		}
	}

	private static final class LifeCycleBuilders
	{
		private static LifecycleSemantics oneToOneLifecycle()
		{
			return LifecycleSemanticsBuilder.lifecycleSemantics()
			                                .cascadeCreate()
			                                .cascadeUpdate()
			                                .hydrateOnFetch()
			                                .build();
		}

		private static LifecycleSemantics manyLifecycle()
		{
			return LifecycleSemanticsBuilder.lifecycleSemantics()
			                                .cascadeCreate()
			                                .cascadeUpdate()
			                                .cascadeDelete()
			                                .orphanDelete()
			                                .hydrateOnFetch()
			                                .build();
		}

		private LifeCycleBuilders()
		{
		}
	}

	@Nested
	@DisplayName("Lifecycle semantics builder")
	final class LifecycleSemanticsBuilderTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("builderCases")
		@DisplayName("lifecycle builder should produce explicit defaults and toggles")
		void lifecycleBuilderShouldProduceExplicitDefaultsAndToggles(final String as, final LifecycleCase tc)
		{
			assertThat(tc.builder().get().build())
					.as("lifecycle semantics should match for %s", as)
					.isEqualTo(tc.expected());
		}

		private static Stream<Arguments> builderCases()
		{
			return Stream.of(
					LifecycleCase.shape("default flags", LifecycleSemanticsBuilder::lifecycleSemantics,
							LifecycleSemantics.none()),
					LifecycleCase.shape("all flags enabled",
							() -> LifecycleSemanticsBuilder.lifecycleSemantics()
							                               .cascadeCreate()
							                               .cascadeUpdate()
							                               .cascadeDelete()
							                               .orphanDelete()
							                               .hydrateOnFetch(),
							LifecycleSemantics.of(true, true, true, true, true))
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record LifecycleCase(
				String as,
				Supplier<LifecycleSemanticsBuilder> builder,
				LifecycleSemantics expected)
		{
			private static LifecycleCase shape(
					final String as,
					final Supplier<LifecycleSemanticsBuilder> builder,
					final LifecycleSemantics expected)
			{
				return new LifecycleCase(as, builder, expected);
			}
		}
	}

	@Nested
	@DisplayName("Aggregate and relationship builders")
	final class AggregateAndRelationshipBuilderTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("builderCases")
		@DisplayName("builder-produced definitions should drive the real engine and services")
		void builderProducedDefinitionsShouldDriveTheRealEngineAndServices(final String as, final BuilderCase tc)
		{
			var scenario = new TestScenario();
			tc.configure().accept(scenario);

			assertThat(tc.assertion().apply(scenario))
					.as("builder-driven behavior should match for %s", as)
					.isTrue();
		}

		private static Stream<Arguments> builderCases()
		{
			return Stream.of(
					BuilderCase.shape(
							"zero-relationship aggregate definition",
							_ ->
							{
							},
							scenario ->
							{
								var saved = scenario.saveService().save(new MasterCreate("master", List.of()));
								return saved.model().value().equals("master");
							}),
					BuilderCase.shape(
							"one-to-one relationship built through the DSL",
							scenario -> scenario.installRelationship(
									Cardinality.ONE,
									ReconciliationStrategy.REPLACE,
									LifeCycleBuilders.oneToOneLifecycle(),
									SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER),
							scenario ->
							{
								var saved = scenario.saveService().save(new MasterCreate(
										"master",
										List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
												new SatelliteCreate("satellite")))));
								var fetched = scenario.fetchService().findById(saved.id());
								return fetched.model().satelliteDomainIds().equals(List.of(1L))
										&& fetched.model().hydratedSatellites().stream()
										          .map(SatelliteModel::value)
										          .toList()
										          .equals(List.of("satellite"));
							}),
					BuilderCase.shape(
							"many MERGE_BY_ID relationship built through the DSL",
							scenario ->
							{
								scenario.installRelationship(
										Cardinality.MANY,
										ReconciliationStrategy.MERGE_BY_ID,
										LifeCycleBuilders.manyLifecycle(),
										SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
								scenario.masterStore.put("master-1",
										new MasterModel("master", List.of(1L, 2L), List.of()));
								scenario.satelliteStore.put(1L, new SatelliteModel("one"));
								scenario.satelliteStore.put(2L, new SatelliteModel("two"));
							},
							scenario ->
							{
								var updated = scenario.updateService().updateById(
										"master-1",
										new MasterPatch(
												null,
												List.of(
														new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
																new SatellitePatch("one-updated")),
														new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L),
														new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
																new SatelliteCreate("three")))));
								var fetched = scenario.fetchService().findById("master-1");
								return updated.model().satelliteDomainIds().size() == 2
										&& fetched.model().hydratedSatellites().stream()
										          .map(SatelliteModel::value)
										          .toList()
										          .equals(List.of("one-updated", "three"));
							})
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record BuilderCase(
				String as,
				java.util.function.Consumer<TestScenario> configure,
				java.util.function.Function<TestScenario, Boolean> assertion)
		{
			private static BuilderCase shape(
					final String as,
					final java.util.function.Consumer<TestScenario> configure,
					final java.util.function.Function<TestScenario, Boolean> assertion)
			{
				return new BuilderCase(as, configure, assertion);
			}
		}
	}
}