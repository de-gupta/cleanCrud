package de.gupta.clean.crud.template.domain.aggregate.intent;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.execution.*;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SatelliteCreateIntentResolverTest
{
	@Test
	void resolvesReferenceAndInlineCreateIntents()
	{
		var scenario = new TestScenario(LifecycleSemantics.of(true, false, false, false, false));
		scenario.satellites.put(7L, new SatelliteModel("existing"));

		var ids = scenario.resolver.resolveSatelliteIdsForCreate(
				scenario.relationship(),
				new MasterCreate(List.of(
						new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(7L),
						new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(new SatelliteCreate("created")))));

		assertEquals(List.of(7L, 1L), ids);
		assertEquals("created", scenario.satellites.get(1L).value());
	}

	@Test
	void rejectsMissingReferencedSatellite()
	{
		var scenario = new TestScenario(LifecycleSemantics.of(true, false, false, false, false));

		assertThrows(
				ResourceNotFoundException.class,
				() -> scenario.resolver.resolveSatelliteIdsForCreate(
						scenario.relationship(),
						new MasterCreate(List.of(new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(99L)))));
	}

	@Test
	void rejectsInlineCreateWhenCascadeCreateIsDisabled()
	{
		var scenario = new TestScenario(LifecycleSemantics.none());

		var exception = assertThrows(
				AggregateRelationshipExecutionNotSupportedException.class,
				() -> scenario.resolver.resolveSatelliteIdsForCreate(
						scenario.relationship(),
						new MasterCreate(List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
								new SatelliteCreate("created"))))));

		assertEquals(
				"Relationship 'satellite' does not allow satellite create participation",
				exception.getMessage());
	}

	@Test
	void ignoresNoSatelliteCreateIntent()
	{
		var scenario = new TestScenario(LifecycleSemantics.of(true, false, false, false, false));

		var ids = scenario.resolver.resolveSatelliteIdsForCreate(
				scenario.relationship(),
				new MasterCreate(List.of(new SatelliteCreateIntent.NoSatelliteCreateIntent<>())));

		Assertions.assertTrue(ids.isEmpty());
	}

	@Test
	void usesCustomSatelliteCreateValidatorWhenProvided()
	{
		var scenario = new TestScenario(LifecycleSemantics.of(true, false, false, false, false));
		var resolver = SatelliteCreateIntentResolver.with(
				new SatelliteRelationshipPlanner(),
				new SatelliteReferenceResolver(),
				new SatelliteCreateValidator()
				{
					@Override
					public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
							SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
					void validate(
							final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel,
									MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId,
									SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> relationship,
							final SatelliteDomainModel satelliteDomainModel)
					{
						if (((SatelliteModel) satelliteDomainModel).value().equals("blocked"))
						{
							throw new IllegalStateException("blocked by custom validator");
						}
					}
				});

		var exception = assertThrows(
				IllegalStateException.class,
				() -> resolver.resolveSatelliteIdsForCreate(
						scenario.relationship(),
						new MasterCreate(List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
								new SatelliteCreate("blocked"))))));

		assertEquals("blocked by custom validator", exception.getMessage());
	}

	private record MasterCreate(Collection<SatelliteCreateIntent<Long, SatelliteCreate>> intents)
	{
	}

	private record MasterModel(List<Long> satelliteIds)
	{
	}

	private record MasterPatch()
	{
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

	private static final class TestScenario
	{
		private final Map<Long, SatelliteModel> satellites = new LinkedHashMap<>();
		private final AtomicLong generatedIds = new AtomicLong();
		private final SatelliteCreateIntentResolver resolver =
				SatelliteCreateIntentResolver.with(new SatelliteRelationshipPlanner(),
						new SatelliteReferenceResolver());
		private final AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
				satelliteDefinition;
		private final AggregateRelationshipDefinition<String, MasterModel, MasterCreate, MasterPatch, Long,
				SatelliteModel, SatelliteCreate, SatellitePatch> relationship;

		private AggregateRelationshipDefinition<String, MasterModel, MasterCreate, MasterPatch, Long,
				SatelliteModel, SatelliteCreate, SatellitePatch> relationship()
		{
			return relationship;
		}

		private TestScenario(final LifecycleSemantics lifecycleSemantics)
		{
			this.satelliteDefinition = satelliteDefinition();
			this.relationship = relationship(lifecycleSemantics);
		}

		private AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String> satelliteDefinition()
		{
			return new AggregateDefinition<>()
			{
				@Override
				public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort()
				{
					return new AggregateMutationPort<>()
					{
						@Override
						public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
						{
							var id = generatedIds.incrementAndGet();
							satellites.put(id, domainModel);
							return IdentifiedModel.of(id, domainModel);
						}

						@Override
						public void put(final Long domainId, final SatelliteModel domainModel)
						{
							satellites.put(domainId, domainModel);
						}

						@Override
						public IdentifiedModel<Long, SatelliteModel> update(final Long domainId,
						                                                    final SatelliteModel domainModel)
						{
							satellites.put(domainId, domainModel);
							return IdentifiedModel.of(domainId, domainModel);
						}

						@Override
						public void delete(final Long domainId)
						{
							satellites.remove(domainId);
						}
					};
				}

				@Override
				public AggregateFetchPort<Long, SatelliteModel> fetchPort()
				{
					return new AggregateFetchPort<>()
					{
						@Override
						public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long domainId)
						{
							return Optional.ofNullable(satellites.get(domainId))
							               .map(model -> IdentifiedModel.of(domainId, model));
						}

						@Override
						public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> domainIds)
						{
							return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
						}

						@Override
						public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
						{
							return satellites.entrySet().stream()
							                 .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
							                 .toList();
						}

						@Override
						public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
						{
							return new SliceImpl<>(findAll().stream().toList());
						}
					};
				}

				@Override
				public de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder<SatelliteCreate, SatelliteModel> createBuilder()
				{
					return create -> new SatelliteModel(create.value());
				}

				@Override
				public de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher<SatelliteModel, SatellitePatch> patcher()
				{
					return (_, patch) -> new SatelliteModel(patch.value());
				}

				@Override
				public de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder<SatelliteModel, String> responseBuilder()
				{
					return SatelliteModel::value;
				}

				@Override
				public DeletionPolicy<SatelliteModel> deletionPolicy()
				{
					return _ ->
					{
					};
				}

				@Override
				public DuplicateDefinition<SatelliteModel> duplicateDefinition()
				{
					return (left, right) -> left.value().equals(right.value());
				}

				@Override
				public PostCommitMutation<Long, SatelliteModel> postCommitMutation()
				{
					return PostCommitMutation.noop();
				}

				@Override
				public Collection<AggregateRelationshipDefinitionContract<Long, SatelliteModel, SatelliteCreate, SatellitePatch>> relationshipDefinitions()
				{
					return List.of();
				}

				@Override
				public DomainSecurityPolicy<SatelliteModel> securityPolicy()
				{
					return DomainSecurityPolicy.allowing();
				}

				@Override
				public PatchPolicy<SatelliteModel> patchPolicy()
				{
					return (_, _) ->
					{
					};
				}

				@Override
				public InsertionPolicy<SatelliteModel> insertionPolicy()
				{
					return _ ->
					{
					};
				}
			};
		}

		private AggregateRelationshipDefinition<String, MasterModel, MasterCreate, MasterPatch, Long,
				SatelliteModel, SatelliteCreate, SatellitePatch> relationship(
				final LifecycleSemantics lifecycleSemantics)
		{
			return new AggregateRelationshipDefinition<>()
			{
				@Override
				public String name()
				{
					return "satellite";
				}

				@Override
				public Cardinality cardinality()
				{
					return Cardinality.MANY;
				}

				@Override
				public RelationshipKind relationshipKind()
				{
					return RelationshipKind.OWNED;
				}

				@Override
				public LifecycleSemantics lifecycleSemantics()
				{
					return lifecycleSemantics;
				}

				@Override
				public ReconciliationStrategy reconciliationStrategy()
				{
					return ReconciliationStrategy.REPLACE;
				}

				@Override
				public AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, ?> satelliteDefinition()
				{
					return satelliteDefinition;
				}

				@Override
				public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> satelliteMutationPort()
				{
					return satelliteDefinition.mutationPort();
				}

				@Override
				public AggregateFetchPort<Long, SatelliteModel> satelliteFetchPort()
				{
					return satelliteDefinition.fetchPort();
				}

				@Override
				public SatelliteCreateInputResolver<MasterCreate, Collection<SatelliteCreateIntent<Long, SatelliteCreate>>> createInputResolver()
				{
					return MasterCreate::intents;
				}

				@Override
				public SatellitePatchInputResolver<MasterPatch, Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>>> patchInputResolver()
				{
					return _ -> List.of();
				}

				@Override
				public SatelliteIdentityResolver<MasterModel, SatelliteModel, Long> identityResolver()
				{
					return (_, _) -> Optional.empty();
				}

				@Override
				public SatelliteLinkStrategy<String, MasterModel, Long, SatelliteModel> linkStrategy()
				{
					return new SatelliteLinkStrategy<>()
					{
						@Override
						public SatellitePersistenceOrder persistenceOrder()
						{
							return SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
						}

						@Override
						public Optional<Long> currentLinkedSatelliteDomainId(final MasterModel masterDomainModel)
						{
							return masterDomainModel.satelliteIds().stream().findFirst();
						}

						@Override
						public Collection<Long> currentLinkedSatelliteDomainIds(final MasterModel masterDomainModel)
						{
							return masterDomainModel.satelliteIds();
						}

						@Override
						public MasterModel replaceLinkedSatelliteDomainIds(final MasterModel masterDomainModel,
						                                                   final Collection<Long> satelliteDomainIds)
						{
							return new MasterModel(List.copyOf(satelliteDomainIds));
						}

						@Override
						public MasterModel attachHydratedSatellites(final MasterModel masterDomainModel,
						                                            final Collection<IdentifiedModel<Long, SatelliteModel>> satellites)
						{
							return masterDomainModel;
						}
					};
				}

				@Override
				public SatelliteHydrationStrategy<String, MasterModel, Long, SatelliteModel> hydrationStrategy()
				{
					return (master, _, _) -> master.model();
				}
			};
		}
	}
}