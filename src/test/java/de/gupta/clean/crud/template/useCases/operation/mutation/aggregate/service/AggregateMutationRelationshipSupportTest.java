package de.gupta.clean.crud.template.useCases.operation.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler.RegisteredMutationHandler;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.plan.AggregateMutationPlan;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMutationRelationshipSupportTest
{
	@Test
	void aggregateMutationCanChangeRootAndOwnedSatellitesTogether()
	{
		var definition = new MasterAggregateDefinition(RelationshipKind.OWNED);
		definition.masterStore.put("order-1", new MasterModel("SUBMITTED", List.of()));
		var service = mutationService(definition, registry());

		var updated = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeWithOwnedNote("printed"),
				OperationSource.INTERNAL_COMMAND));

		assertEquals("ACKNOWLEDGED", updated.model().status());
		assertEquals(1, updated.model().noteIds().size());
		assertEquals("printed", definition.satelliteStore.get(updated.model().noteIds().getFirst()).value());
	}

	@Test
	void aggregateMutationRejectsReferencedRelationshipMutationPlans()
	{
		var definition = new MasterAggregateDefinition(RelationshipKind.REFERENCED);
		definition.masterStore.put("order-1", new MasterModel("SUBMITTED", List.of()));
		definition.satelliteStore.put(7L, new SatelliteModel("external"));
		var service = mutationService(definition, referencedRegistry());

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new LinkReferencedNote(7L),
						OperationSource.INTERNAL_COMMAND)));

		assertTrue(exception.getMessage().contains("mutate the referenced aggregate directly"));
	}

	private MutationService<String, MasterModel> mutationService(
			final MasterAggregateDefinition definition,
			final MutationHandlerRegistry<MasterModel> registry)
	{
		var engine =
				de.gupta.clean.crud.template.useCases.crud.aggregate.engine.DefaultAggregateLifecycleEngine.withTransactionRunner(
						new InlineTransactionRunner());
		return AggregateMutationServices.mutationService(definition, engine, registry);
	}

	private MutationHandlerRegistry<MasterModel> registry()
	{
		return MutationHandlerRegistry.of(List.of(
				RegisteredMutationHandler.ofAggregate(
						AcknowledgeWithOwnedNote.class,
						(currentModel, payload) -> AggregateMutationPlan.builder(
																				new MasterModel("ACKNOWLEDGED", currentModel.noteIds()))
						                                                .mutateRelationship(
																				"note",
																				List.of(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
																						new SatelliteCreate(
																								payload.noteValue()))))
						                                                .build())));
	}

	private MutationHandlerRegistry<MasterModel> referencedRegistry()
	{
		return MutationHandlerRegistry.of(List.of(
				RegisteredMutationHandler.ofAggregate(
						LinkReferencedNote.class,
						(currentModel, payload) -> AggregateMutationPlan.builder(currentModel)
						                                                .mutateRelationship(
																				"note",
																				List.of(new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(
																						payload.noteId())))
						                                                .build())));
	}

	private record MasterModel(String status, List<Long> noteIds)
	{
		private MasterModel withNoteIds(final Collection<Long> noteIds)
		{
			return new MasterModel(status, List.copyOf(noteIds));
		}
	}

	private record SatelliteModel(String value)
	{
	}

	private record SatelliteCreate(String value)
	{
	}

	private record SatellitePatch(String value)
	{
	}

	private record AcknowledgeWithOwnedNote(String noteValue) implements ApplicationOperationPayload
	{
	}

	private record LinkReferencedNote(Long noteId) implements ApplicationOperationPayload
	{
	}

	private static final class MasterAggregateDefinition
			implements AggregateCrudDefinition<String, MasterModel, String, String, String>
	{
		private final RelationshipKind relationshipKind;
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final AtomicLong generatedSatelliteIds = new AtomicLong();

		@Override
		public AggregateMutationPort<String, MasterModel, String, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					masterStore.put("created", domainModel);
					return IdentifiedModel.of("created", domainModel);
				}

				@Override
				public void put(final String domainId, final MasterModel domainModel)
				{
					masterStore.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String domainId, final MasterModel domainModel)
				{
					masterStore.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					masterStore.remove(domainId);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, MasterModel> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, MasterModel>> findById(final String domainId)
				{
					return Optional.ofNullable(masterStore.get(domainId))
					               .map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findByIds(final Set<String> domainIds)
				{
					return domainIds.stream().flatMap(id -> findById(id).stream()).toList();
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findAll()
				{
					return masterStore.entrySet().stream()
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					                  .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, MasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		@Override
		public DomainModelBuilder<String, MasterModel> createBuilder()
		{
			return _ -> new MasterModel("CREATED", List.of());
		}

		@Override
		public DomainModelPatcher<MasterModel, String> patcher()
		{
			return (originalDomainModel, patch) -> new MasterModel(patch, originalDomainModel.noteIds());
		}

		@Override
		public DomainResponseBuilder<MasterModel, String> responseBuilder()
		{
			return MasterModel::status;
		}

		@Override
		public InsertionPolicy<MasterModel> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public PatchPolicy<MasterModel> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public DeletionPolicy<MasterModel> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DomainSecurityPolicy<MasterModel> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public DuplicateDefinition<MasterModel> duplicateDefinition()
		{
			return (left, right) -> left.status().equals(right.status()) && left.noteIds().equals(right.noteIds());
		}

		@Override
		public PostCommitMutation<String, MasterModel> postCommitMutation()
		{
			return PostCommitMutation.noop();
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, MasterModel, String, String>>
		relationshipDefinitions()
		{
			return List.of(new NoteRelationshipDefinition());
		}

		private MasterAggregateDefinition(final RelationshipKind relationshipKind)
		{
			this.relationshipKind = relationshipKind;
		}

		private final class NoteRelationshipDefinition
				implements AggregateRelationshipDefinition<String, MasterModel, String, String, Long, SatelliteModel,
				SatelliteCreate, SatellitePatch>
		{
			@Override
			public String name()
			{
				return "note";
			}

			@Override
			public Cardinality cardinality()
			{
				return Cardinality.MANY;
			}

			@Override
			public RelationshipKind relationshipKind()
			{
				return relationshipKind;
			}

			@Override
			public LifecycleSemantics lifecycleSemantics()
			{
				return LifecycleSemantics.of(true, true, false, true, false);
			}

			@Override
			public ReconciliationStrategy reconciliationStrategy()
			{
				return ReconciliationStrategy.MERGE_BY_ID;
			}

			@Override
			public AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, ?> satelliteDefinition()
			{
				return new SatelliteAggregateDefinition();
			}

			@Override
			public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> satelliteMutationPort()
			{
				return satelliteDefinition().mutationPort();
			}

			@Override
			public AggregateFetchPort<Long, SatelliteModel> satelliteFetchPort()
			{
				return satelliteDefinition().fetchPort();
			}

			@Override
			public SatelliteCreateInputResolver<String, Collection<SatelliteCreateIntent<Long, SatelliteCreate>>> createInputResolver()
			{
				return _ -> List.of();
			}

			@Override
			public SatellitePatchInputResolver<String, Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>>>
			patchInputResolver()
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
						return SatellitePersistenceOrder.NO_ORDER_CONSTRAINT;
					}

					@Override
					public Optional<Long> currentLinkedSatelliteDomainId(final MasterModel masterDomainModel)
					{
						return masterDomainModel.noteIds().stream().findFirst();
					}

					@Override
					public Collection<Long> currentLinkedSatelliteDomainIds(final MasterModel masterDomainModel)
					{
						return masterDomainModel.noteIds();
					}

					@Override
					public MasterModel replaceLinkedSatelliteDomainIds(
							final MasterModel masterDomainModel,
							final Collection<Long> satelliteDomainIds)
					{
						return masterDomainModel.withNoteIds(satelliteDomainIds);
					}

					@Override
					public MasterModel attachHydratedSatellites(
							final MasterModel masterDomainModel,
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
		}

		private final class SatelliteAggregateDefinition
				implements AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
		{
			@Override
			public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort()
			{
				return new AggregateMutationPort<>()
				{
					@Override
					public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
					{
						var id = generatedSatelliteIds.incrementAndGet();
						satelliteStore.put(id, domainModel);
						return IdentifiedModel.of(id, domainModel);
					}

					@Override
					public void put(final Long domainId, final SatelliteModel domainModel)
					{
						satelliteStore.put(domainId, domainModel);
					}

					@Override
					public IdentifiedModel<Long, SatelliteModel> update(final Long domainId,
					                                                    final SatelliteModel domainModel)
					{
						satelliteStore.put(domainId, domainModel);
						return IdentifiedModel.of(domainId, domainModel);
					}

					@Override
					public void delete(final Long domainId)
					{
						satelliteStore.remove(domainId);
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
						return Optional.ofNullable(satelliteStore.get(domainId))
						               .map(model -> IdentifiedModel.of(domainId, model));
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> domainIds)
					{
						return domainIds.stream().flatMap(id -> findById(id).stream()).toList();
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
					{
						return satelliteStore.entrySet().stream()
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
			public DomainModelBuilder<SatelliteCreate, SatelliteModel> createBuilder()
			{
				return create -> new SatelliteModel(create.value());
			}

			@Override
			public DomainModelPatcher<SatelliteModel, SatellitePatch> patcher()
			{
				return (_, patch) -> new SatelliteModel(patch.value());
			}

			@Override
			public DomainResponseBuilder<SatelliteModel, String> responseBuilder()
			{
				return SatelliteModel::value;
			}

			@Override
			public InsertionPolicy<SatelliteModel> insertionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public PatchPolicy<SatelliteModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public DeletionPolicy<SatelliteModel> deletionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public DomainSecurityPolicy<SatelliteModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
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
			public Collection<AggregateRelationshipDefinitionContract<Long, SatelliteModel, SatelliteCreate, SatellitePatch>>
			relationshipDefinitions()
			{
				return List.of();
			}
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
}