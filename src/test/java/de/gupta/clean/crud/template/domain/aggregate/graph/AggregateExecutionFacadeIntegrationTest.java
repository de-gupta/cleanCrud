package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.aggregate.runtime.AggregateWorkflowRunner;
import de.gupta.clean.crud.template.domain.aggregate.runtime.DefaultAggregateWorkflowRunner;
import de.gupta.clean.crud.template.domain.aggregate.workflow.AggregateWorkflow;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceCannotBeDeletedException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Aggregate execution facade")
final class AggregateExecutionFacadeIntegrationTest
{
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

	private record MasterModel(String value, List<Long> satelliteDomainIds, List<SatelliteModel> hydratedSatellites)
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
		private final TestTransactionRunner transactionRunner = new TestTransactionRunner();
		private final TestEngineFacade engine;
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final List<String> operationLog = new ArrayList<>();
		private final TestMasterAggregateDefinition masterDefinition;
		private final TestSatelliteAggregateDefinition satelliteDefinition;

		private void installRelationship(
				final Cardinality cardinality,
				final ReconciliationStrategy reconciliationStrategy,
				final LifecycleSemantics lifecycleSemantics,
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			masterDefinition.relationshipDefinitions = List.of(
					new TestRelationshipDefinition(cardinality, reconciliationStrategy, lifecycleSemantics,
							satellitePersistenceOrder));
		}

		private TestScenario(
				final List<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>> relationshipDefinitions)
		{
			this.satelliteDefinition = new TestSatelliteAggregateDefinition();
			this.masterDefinition = new TestMasterAggregateDefinition(relationshipDefinitions);
			AggregateWorkflowRunner workflowEngine =
					DefaultAggregateWorkflowRunner.withTransactionRunner(transactionRunner);
			this.engine = new TestEngineFacade(workflowEngine);
		}

		private final class TestMasterAggregateDefinition
				implements AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, String>
		{
			private final AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> mutationPort =
					new TestMasterMutationPort();
			private final AggregateFetchPort<String, MasterModel> fetchPort = new TestMasterFetchPort();
			private final AtomicInteger generatedIds = new AtomicInteger();
			private final PostCommitMutation<String, MasterModel> postCommitMutation = PostCommitMutation.noop();
			private Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
					relationshipDefinitions;
			private DeletionPolicy<MasterModel> deletionPolicy = _ ->
			{
			};

			@Override
			public AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> mutationPort()
			{
				return mutationPort;
			}

			@Override
			public AggregateFetchPort<String, MasterModel> fetchPort()
			{
				return fetchPort;
			}

			@Override
			public DomainModelBuilder<MasterCreate, MasterModel> createBuilder()
			{
				return create -> new MasterModel(create.value(), List.of(), List.of());
			}

			@Override
			public DomainModelPatcher<MasterModel, MasterPatch> patcher()
			{
				return (originalDomainModel, patch) -> new MasterModel(
						patch.value() == null ? originalDomainModel.value() : patch.value(),
						originalDomainModel.satelliteDomainIds(),
						originalDomainModel.hydratedSatellites());
			}

			@Override
			public DomainResponseBuilder<MasterModel, String> responseBuilder()
			{
				return MasterModel::value;
			}

			@Override
			public DeletionPolicy<MasterModel> deletionPolicy()
			{
				return deletionPolicy;
			}

			@Override
			public DuplicateDefinition<MasterModel> duplicateDefinition()
			{
				return (KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value;
			}

			@Override
			public PostCommitMutation<String, MasterModel> postCommitMutation()
			{
				return postCommitMutation;
			}

			@Override
			public Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
			relationshipDefinitions()
			{
				return relationshipDefinitions;
			}

			@Override
			public DomainSecurityPolicy<MasterModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
			}

			@Override
			public PatchPolicy<MasterModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public InsertionPolicy<MasterModel> insertionPolicy()
			{
				return _ ->
				{
				};
			}

			private TestMasterAggregateDefinition(
					final Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
							relationshipDefinitions)
			{
				this.relationshipDefinitions = relationshipDefinitions;
			}

			private final class TestMasterMutationPort
					implements AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch>
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					var id = "master-" + generatedIds.incrementAndGet();
					operationLog.add("master:create:" + domainModel.value());
					masterStore.put(id, domainModel);
					return IdentifiedModel.of(id, domainModel);
				}

				@Override
				public void put(final String domainId, final MasterModel domainModel)
				{
					operationLog.add("master:put:" + domainId);
					masterStore.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String domainId, final MasterModel domainModel)
				{
					operationLog.add("master:update:" + domainId);
					masterStore.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					operationLog.add("master:delete:" + domainId);
					masterStore.remove(domainId);
				}
			}

			private final class TestMasterFetchPort implements AggregateFetchPort<String, MasterModel>
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
					return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
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
			}
		}

		private final class TestSatelliteAggregateDefinition
				implements AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
		{
			private final AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort =
					new TestSatelliteMutationPort();
			private final AggregateFetchPort<Long, SatelliteModel> fetchPort = new TestSatelliteFetchPort();
			private final AtomicInteger generatedIds = new AtomicInteger();

			@Override
			public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort()
			{
				return mutationPort;
			}

			@Override
			public AggregateFetchPort<Long, SatelliteModel> fetchPort()
			{
				return fetchPort;
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
			public Collection<AggregateRelationshipDefinitionContract<Long, SatelliteModel, SatelliteCreate, SatellitePatch>>
			relationshipDefinitions()
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

			private final class TestSatelliteMutationPort
					implements AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch>
			{
				@Override
				public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
				{
					generatedIds.updateAndGet(current -> Math.max(current, satelliteStore.keySet().stream()
					                                                                     .mapToInt(Long::intValue)
					                                                                     .max()
					                                                                     .orElse(0)));
					var id = (long) generatedIds.incrementAndGet();
					operationLog.add("satellite:create:" + domainModel.value());
					satelliteStore.put(id, domainModel);
					return IdentifiedModel.of(id, domainModel);
				}

				@Override
				public void put(final Long domainId, final SatelliteModel domainModel)
				{
					operationLog.add("satellite:put:" + domainId);
					satelliteStore.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, SatelliteModel> update(final Long domainId,
				                                                    final SatelliteModel domainModel)
				{
					operationLog.add("satellite:update:" + domainId);
					satelliteStore.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final Long domainId)
				{
					operationLog.add("satellite:delete:" + domainId);
					satelliteStore.remove(domainId);
				}
			}

			private final class TestSatelliteFetchPort implements AggregateFetchPort<Long, SatelliteModel>
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
					return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
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
			}
		}

		private final class TestRelationshipDefinition
				implements AggregateRelationshipDefinition<String, MasterModel, MasterCreate, MasterPatch, Long,
				SatelliteModel, SatelliteCreate, SatellitePatch>
		{
			private final Cardinality cardinality;
			private final ReconciliationStrategy reconciliationStrategy;
			private final LifecycleSemantics lifecycleSemantics;
			private final SatellitePersistenceOrder satellitePersistenceOrder;

			@Override
			public String name()
			{
				return "satellite";
			}

			@Override
			public Cardinality cardinality()
			{
				return cardinality;
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
				return reconciliationStrategy;
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
			public SatelliteCreateInputResolver<MasterCreate, Collection<SatelliteCreateIntent<Long, SatelliteCreate>>>
			createInputResolver()
			{
				return MasterCreate::satelliteCreateIntents;
			}

			@Override
			public SatellitePatchInputResolver<MasterPatch,
					Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>>> patchInputResolver()
			{
				return MasterPatch::satelliteMutationIntents;
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

			@Override
			public SatelliteHydrationStrategy<String, MasterModel, Long, SatelliteModel> hydrationStrategy()
			{
				return (master, satelliteFetchPort, satelliteLinkStrategy) ->
				{
					var hydratedSatellites = new ArrayList<IdentifiedModel<Long, SatelliteModel>>();
					for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(master.model()))
					{
						satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
					}
					return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
				};
			}

			private TestRelationshipDefinition(
					final Cardinality cardinality,
					final ReconciliationStrategy reconciliationStrategy,
					final LifecycleSemantics lifecycleSemantics,
					final SatellitePersistenceOrder satellitePersistenceOrder)
			{
				this.cardinality = cardinality;
				this.reconciliationStrategy = reconciliationStrategy;
				this.lifecycleSemantics = lifecycleSemantics;
				this.satellitePersistenceOrder = satellitePersistenceOrder;
			}
		}
	}

	private static final class TestEngineFacade
	{
		private final AggregateWorkflowRunner engine;
		private final AggregateDefinitionRelationshipInspector definitionGuard;
		private final AggregateMutationValidationSupport validationSupport;
		private final AggregateSaveCoordinator saveCoordinator;
		private final AggregateFetchCoordinator fetchCoordinator;
		private final AggregateDeleteCoordinator deleteCoordinator;
		private final AggregateUpdateCoordinator updateCoordinator;

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> save(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainModelCreate model)
		{
			return saveAll(definition, List.of(model)).stream().findFirst().orElseThrow();
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<MasterDomainModelCreate> models)
		{
			return engine.run(new AggregateWorkflow<>()
			{
				@Override
				public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						var domainModels = models.stream().map(definition.createBuilder()::toModel).toList();
						validationSupport.validateSaveModels(definition, domainModels);
						return domainModels.stream().map(definition.mutationPort()::create).toList();
					}
					validationSupport.validateSaveModels(definition,
							models.stream().map(definition.createBuilder()::toModel).toList());
					return saveCoordinator.saveAll(definition, relationships, models);
				}

				@Override
				public void afterTransaction(
						final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> result)
				{
					result.forEach(saved -> definition.postCommitMutation().accept(createContext(saved)));
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, ?, ?, ?, ?>> satelliteRelationships(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition)
		{
			return definitionGuard.satelliteRelationships(definition);
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> createContext(
				final IdentifiedModel<MasterDomainId, MasterDomainModel> saved)
		{
			return new PostCommitMutationContext<>(
					PostCommitMutationKind.CREATE,
					saved.id(),
					Optional.of(saved.model()),
					Optional.empty());
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void putAtId(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelCreate model)
		{
			engine.run(new AggregateWorkflow<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public PostCommitMutationContext<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id).map(IdentifiedModel::model);
					if (relationships.isEmpty())
					{
						var replacement = definition.createBuilder().toModel(model);
						validationSupport.validateAccess(definition, replacement);
						previousModel.ifPresentOrElse(
								current -> validationSupport.validateAccessAndValidatePatch(definition, current,
										replacement),
								() -> definition.insertionPolicy().validateInsertion(replacement));
						definition.mutationPort().put(id, replacement);
					}
					else
					{
						updateCoordinator.putAtId(definition, relationships, id, model);
					}
					var currentModel = definition.fetchPort().findById(id)
					                             .map(IdentifiedModel::model)
					                             .orElseThrow();
					return putContext(id, previousModel, currentModel);
				}

				@Override
				public void afterTransaction(final PostCommitMutationContext<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result);
				}
			});
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> putContext(
				final MasterDomainId id,
				final Optional<MasterDomainModel> previousModel,
				final MasterDomainModel currentModel)
		{
			return new PostCommitMutationContext<>(PostCommitMutationKind.PUT, id, Optional.of(currentModel),
					previousModel);
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> updateAllById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
				final BulkOperationMode mode)
		{
			return switch (mode)
			{
				case ALL_OR_NOTHING -> engine.run(
						new AggregateWorkflow<Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>>>()
						{
							@Override
							public Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> inTransaction()
							{
								var relationships = satelliteRelationships(definition);
								var updates = new ArrayList<UpdateDispatch<MasterDomainId, MasterDomainModel>>();
								for (var model : models)
								{
									var previousModel = definition.fetchPort().findById(model.id())
									                              .map(IdentifiedModel::model)
									                              .orElseThrow(() -> ResourceNotFoundException.withId(
																		  model.id()));
									var updated = relationships.isEmpty()
											? updateByIdWithoutRelationships(definition, model.id(), model.model())
											: updateCoordinator.updateById(definition, relationships, model.id(),
											model.model());
									updates.add(new UpdateDispatch<>(updated,
											patchContext(updated.id(), Optional.of(previousModel), updated.model())));
								}
								return updates;
							}

							@Override
							public void afterTransaction(
									final Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> result)
							{
								result.forEach(update -> definition.postCommitMutation().accept(update.context()));
							}
						}).stream().map(UpdateDispatch::updated).toList();
				case BEST_EFFORT -> models.stream()
				                          .map(model -> tryUpdateById(definition, model.id(), model.model()))
				                          .flatMap(Optional::stream)
				                          .toList();
			};
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> updateByIdWithoutRelationships(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
			validationSupport.validateAccess(definition, current.model());
			var updatedModel = definition.patcher().patchModel(current.model(), updatePatch);
			validationSupport.validateAccessAndValidatePatch(definition, current.model(), updatedModel);
			return definition.mutationPort().update(id, updatedModel);
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> patchContext(
				final MasterDomainId id,
				final Optional<MasterDomainModel> previousModel,
				final MasterDomainModel currentModel)
		{
			return new PostCommitMutationContext<>(PostCommitMutationKind.PATCH, id, Optional.of(currentModel),
					previousModel);
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Optional<IdentifiedModel<MasterDomainId, MasterDomainModel>> tryUpdateById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			try
			{
				return Optional.of(updateById(definition, id, updatePatch));
			}
			catch (DomainException e)
			{
				return Optional.empty();
			}
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> updateById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			return engine.run(new AggregateWorkflow<UpdateDispatch<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public UpdateDispatch<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id)
					                              .map(IdentifiedModel::model)
					                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
					var updated = relationships.isEmpty()
							? updateByIdWithoutRelationships(definition, id, updatePatch)
							: updateCoordinator.updateById(definition, relationships, id, updatePatch);
					return new UpdateDispatch<>(updated, patchContext(updated.id(), Optional.of(previousModel),
							updated.model()));
				}

				@Override
				public void afterTransaction(final UpdateDispatch<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result.context());
				}
			}).updated();
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> findById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			return engine.run(new AggregateWorkflow<>()
			{
				@Override
				public IdentifiedModel<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						return definition.fetchPort().findById(id)
						                 .filter(model -> definition.securityPolicy().isAccessAllowed(model.model()))
						                 .orElseThrow(() -> ResourceNotFoundException.withId(id));
					}
					return fetchCoordinator.findById(definition, relationships, id);
				}

				@Override
				public boolean readOnly()
				{
					return true;
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void deleteAllById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<MasterDomainId> ids,
				final BulkOperationMode mode)
		{
			switch (mode)
			{
				case ALL_OR_NOTHING -> engine.run(
						new AggregateWorkflow<Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>>()
						{
							@Override
							public Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> inTransaction()
							{
								var relationships = satelliteRelationships(definition);
								var deleted =
										new ArrayList<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>();
								for (var id : ids)
								{
									var previousModel = definition.fetchPort().findById(id)
									                              .map(IdentifiedModel::model)
									                              .orElseThrow(() -> ResourceNotFoundException.withId(
																		  id));
									if (relationships.isEmpty())
									{
										validationSupport.validateDeletion(definition, previousModel);
										definition.mutationPort().delete(id);
									}
									else
									{
										deleteCoordinator.deleteById(definition, relationships, id);
									}
									deleted.add(deleteContext(id, previousModel));
								}
								return deleted;
							}

							@Override
							public void afterTransaction(
									final Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> result)
							{
								result.forEach(definition.postCommitMutation());
							}
						});
				case BEST_EFFORT -> ids.forEach(id -> tryDeleteById(definition, id));
			}
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> deleteContext(
				final MasterDomainId id,
				final MasterDomainModel previousModel)
		{
			return new PostCommitMutationContext<>(PostCommitMutationKind.DELETE, id, Optional.empty(),
					Optional.of(previousModel));
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void tryDeleteById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			try
			{
				deleteById(definition, id);
			}
			catch (DomainException ignored)
			{
			}
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void deleteById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			engine.run(new AggregateWorkflow<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public PostCommitMutationContext<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id)
					                              .map(IdentifiedModel::model)
					                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
					if (relationships.isEmpty())
					{
						validationSupport.validateDeletion(definition, previousModel);
						definition.mutationPort().delete(id);
					}
					else
					{
						deleteCoordinator.deleteById(definition, relationships, id);
					}
					return deleteContext(id, previousModel);
				}

				@Override
				public void afterTransaction(final PostCommitMutationContext<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result);
				}
			});
		}

		private TestEngineFacade(final AggregateWorkflowRunner engine)
		{
			this.engine = engine;
			this.definitionGuard = new AggregateDefinitionRelationshipInspector();
			this.validationSupport = new AggregateMutationValidationSupport();
			var relationshipPlanner = new SatelliteRelationshipPlanner();
			var referenceResolver = new SatelliteReferenceResolver();
			var createIntentResolver = SatelliteCreateIntentResolver.with(relationshipPlanner, referenceResolver);
			this.saveCoordinator = AggregateSaveCoordinator.with(relationshipPlanner, createIntentResolver);
			this.fetchCoordinator = AggregateFetchCoordinator.create();
			this.deleteCoordinator = AggregateDeleteCoordinator.with(relationshipPlanner, referenceResolver);
			this.updateCoordinator =
					AggregateUpdateCoordinator.with(relationshipPlanner, referenceResolver, createIntentResolver,
							validationSupport);
		}

		private record UpdateDispatch<DomainId, DomainModel>(
				IdentifiedModel<DomainId, DomainModel> updated,
				PostCommitMutationContext<DomainId, DomainModel> context)
		{
		}
	}

	private static final class TestTransactionRunner implements PersistenceTransactionRunner
	{
		private int transactionCount;

		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			transactionCount++;
			return action.get();
		}

		int transactionCount()
		{
			return transactionCount;
		}
	}

	@Nested
	@DisplayName("when saving")
	final class WhenSaving
	{
		@Test
		@DisplayName("it still supports zero-relationship CRUD")
		void itStillSupportsZeroRelationshipCrud()
		{
			var scenario = new TestScenario(List.of());

			var saved = scenario.engine.save(scenario.masterDefinition, new MasterCreate("alpha", List.of()));

			assertThat(scenario.transactionRunner.transactionCount()).isEqualTo(1);
			assertThat(saved.id()).isEqualTo("master-1");
			assertThat(saved.model().value()).isEqualTo("alpha");
		}

		@Test
		@DisplayName("it executes one-to-one save, fetch, and delete lifecycle")
		void itExecutesOneToOneSaveFetchAndDeleteLifecycle()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, true, true, true),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

			var saved = scenario.engine.save(
					scenario.masterDefinition,
					new MasterCreate(
							"master",
							List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
									new SatelliteCreate("sat")))));

			assertThat(saved.model().satelliteDomainIds()).containsExactly(1L);
			assertThat(scenario.operationLog.subList(0, 2)).containsExactly("satellite:create:sat",
					"master:create:master");

			var fetched = scenario.engine.findById(scenario.masterDefinition, saved.id());

			assertThat(fetched.model().hydratedSatellites()).hasSize(1);
			assertThat(fetched.model().hydratedSatellites().getFirst().value()).isEqualTo("sat");

			scenario.engine.deleteById(scenario.masterDefinition, saved.id());

			assertThat(scenario.masterStore).doesNotContainKey(saved.id());
			assertThat(scenario.satelliteStore).doesNotContainKey(1L);
		}

		@Test
		@DisplayName("it supports reference satellite create intents")
		void itSupportsReferenceSatelliteCreateIntents()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, true, true, true),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.satelliteStore.put(7L, new SatelliteModel("existing"));

			var saved = scenario.engine.save(
					scenario.masterDefinition,
					new MasterCreate(
							"master",
							List.of(new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(7L))));

			assertThat(saved.model().satelliteDomainIds()).containsExactly(7L);
			assertThat(scenario.satelliteStore.get(7L).value()).isEqualTo("existing");
		}

		@Test
		@DisplayName("it rejects duplicate save-all request items before persistence")
		void itRejectsDuplicateSaveAllRequestItemsBeforePersistence()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, false, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

			assertThatThrownBy(() -> scenario.engine.saveAll(
					scenario.masterDefinition,
					List.of(
							new MasterCreate(
									"duplicate",
									List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
											new SatelliteCreate("sat-one")))),
							new MasterCreate(
									"duplicate",
									List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
											new SatelliteCreate("sat-two")))))))
					.isInstanceOf(InvalidRequestException.class);

			assertThat(scenario.masterStore).isEmpty();
			assertThat(scenario.satelliteStore).isEmpty();
		}

		@Test
		@DisplayName("it rejects merge-by-id for one relationships")
		void itRejectsMergeByIdForOneRelationships()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.MERGE_BY_ID,
					LifecycleSemantics.of(true, true, false, false, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

			assertThatThrownBy(() -> scenario.engine.save(
					scenario.masterDefinition,
					new MasterCreate(
							"master",
							List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
									new SatelliteCreate("satellite"))))))
					.isInstanceOf(AggregateRelationshipExecutionNotSupportedException.class);
		}
	}

	@Nested
	@DisplayName("when updating")
	final class WhenUpdating
	{
		@Test
		@DisplayName("it supports one-to-one reference, create, update, and remove intents")
		void itSupportsOneToOneReferenceCreateUpdateAndRemoveIntents()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, true, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("old"));

			scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
									new SatellitePatch("updated")))));

			assertThat(scenario.satelliteStore.get(1L).value()).isEqualTo("updated");

			scenario.satelliteStore.put(2L, new SatelliteModel("other"));
			var referenced = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(2L))));

			assertThat(referenced.model().satelliteDomainIds()).containsExactly(2L);
			assertThat(scenario.satelliteStore).doesNotContainKey(1L);

			var created = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
									new SatelliteCreate("new")))));
			var createdSatelliteDomainId = created.model().satelliteDomainIds().getFirst();

			assertThat(scenario.satelliteStore.get(createdSatelliteDomainId).value()).isEqualTo("new");

			var removed = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(
									createdSatelliteDomainId))));

			assertThat(removed.model().satelliteDomainIds()).isEmpty();
			assertThat(scenario.satelliteStore).doesNotContainKey(createdSatelliteDomainId);
		}

		@Test
		@DisplayName("it updates the single currently linked satellite")
		void itUpdatesTheSingleCurrentlyLinkedSatellite()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, true, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("old"));

			var updated = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<>(
									new SatellitePatch("updated")))));

			assertThat(updated.model().satelliteDomainIds()).containsExactly(1L);
			assertThat(scenario.satelliteStore.get(1L).value()).isEqualTo("updated");
		}

		@Test
		@DisplayName("it supports many-cardinality replace and merge-by-id")
		void itSupportsManyCardinalityReplaceAndMergeById()
		{
			var replaceScenario = new TestScenario(List.of());
			replaceScenario.installRelationship(
					Cardinality.MANY,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, true, true, true),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			replaceScenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
			replaceScenario.satelliteStore.put(1L, new SatelliteModel("one"));
			replaceScenario.satelliteStore.put(2L, new SatelliteModel("two"));

			var replaced = replaceScenario.engine.updateById(
					replaceScenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(
									new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
											new SatellitePatch("one-updated")),
									new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
											new SatelliteCreate("three")))));

			assertThat(replaceScenario.satelliteStore.get(1L).value()).isEqualTo("one-updated");
			assertThat(replaceScenario.satelliteStore).doesNotContainKey(2L);
			assertThat(replaced.model().satelliteDomainIds()).hasSize(2);

			var mergeScenario = new TestScenario(List.of());
			mergeScenario.installRelationship(
					Cardinality.MANY,
					ReconciliationStrategy.MERGE_BY_ID,
					LifecycleSemantics.of(true, true, true, true, true),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			mergeScenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
			mergeScenario.satelliteStore.put(1L, new SatelliteModel("one"));
			mergeScenario.satelliteStore.put(2L, new SatelliteModel("two"));

			var merged = mergeScenario.engine.updateById(
					mergeScenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(
									new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
											new SatellitePatch("one-updated")),
									new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L),
									new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
											new SatelliteCreate("three")))));

			assertThat(merged.model().satelliteDomainIds()).contains(1L).hasSize(2);
			assertThat(mergeScenario.satelliteStore).doesNotContainKey(2L);
		}

		@Test
		@DisplayName("it unlinks removed satellites when replace does not orphan-delete")
		void itUnlinksRemovedSatellitesWhenReplaceDoesNotOrphanDelete()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.MANY,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, false, true),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("one"));
			scenario.satelliteStore.put(2L, new SatelliteModel("two"));

			var updated = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(1L))));

			assertThat(updated.model().satelliteDomainIds()).containsExactly(1L);
			assertThat(scenario.satelliteStore).containsKey(2L);
		}

		@Test
		@DisplayName("it rejects implicit current-satellite mutations for many relationships")
		void itRejectsImplicitCurrentSatelliteMutationsForManyRelationships()
		{
			for (var reconciliationStrategy : List.of(ReconciliationStrategy.REPLACE,
					ReconciliationStrategy.MERGE_BY_ID))
			{
				var scenario = new TestScenario(List.of());
				scenario.installRelationship(
						Cardinality.MANY,
						reconciliationStrategy,
						LifecycleSemantics.of(true, true, false, false, false),
						SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
				scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
				scenario.satelliteStore.put(1L, new SatelliteModel("one"));
				scenario.satelliteStore.put(2L, new SatelliteModel("two"));

				List<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>> mutationIntents = List.of(
						new SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<>(
								new SatelliteCreate("created"),
								new SatellitePatch("patched")),
						new SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<>(
								new SatellitePatch("patched")),
						new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>());
				for (var mutationIntent : mutationIntents)
				{
					assertThatThrownBy(() -> scenario.engine.updateById(
							scenario.masterDefinition,
							"master-1",
							new MasterPatch(null, List.of(mutationIntent))))
							.isInstanceOf(InvalidRequestException.class)
							.hasMessage(
									"Relationship 'satellite' cannot use implicit current satellite mutations for MANY cardinality; use explicit satellite ids instead");
				}

				assertThat(scenario.masterStore.get("master-1").satelliteDomainIds()).containsExactly(1L, 2L);
				assertThat(scenario.satelliteStore).containsKeys(1L, 2L);
			}
		}

		@Test
		@DisplayName("it removes the single current satellite under replace")
		void itRemovesTheSingleCurrentSatelliteUnderReplace()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, true, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("one"));

			var updated = scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>())));

			assertThat(updated.model().satelliteDomainIds()).isEmpty();
			assertThat(scenario.satelliteStore).doesNotContainKey(1L);
		}

		@Test
		@DisplayName("it requires orphan-delete for replace remove-current")
		void itRequiresOrphanDeleteForReplaceRemoveCurrent()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, false, false, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("one"));

			assertThatThrownBy(() -> scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>()))))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessage(
							"Relationship 'satellite' cannot remove the current satellite under REPLACE unless orphanDelete is enabled");

			assertThat(scenario.masterStore.get("master-1").satelliteDomainIds()).containsExactly(1L);
			assertThat(scenario.satelliteStore).containsKey(1L);
		}

		@Test
		@DisplayName("it requires currently linked ids for explicit update and remove")
		void itRequiresCurrentlyLinkedIdsForExplicitUpdateAndRemove()
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.MANY,
					ReconciliationStrategy.MERGE_BY_ID,
					LifecycleSemantics.of(true, true, false, false, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("one"));
			scenario.satelliteStore.put(2L, new SatelliteModel("two"));

			assertThatThrownBy(() -> scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(2L,
									new SatellitePatch("updated"))))))
					.isInstanceOf(InvalidRequestException.class);

			assertThatThrownBy(() -> scenario.engine.updateById(
					scenario.masterDefinition,
					"master-1",
					new MasterPatch(
							null,
							List.of(new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L)))))
					.isInstanceOf(InvalidRequestException.class);
		}

		@Test
		@DisplayName("it updates all requested models in one all-or-nothing batch")
		void itUpdatesAllRequestedModelsInOneAllOrNothingBatch()
		{
			var scenario = new TestScenario(List.of());
			scenario.masterStore.put("first", new MasterModel("first", List.of(), List.of()));
			scenario.masterStore.put("second", new MasterModel("second", List.of(), List.of()));

			var updated = scenario.engine.updateAllById(
					scenario.masterDefinition,
					List.of(
							IdentifiedModel.of("first", new MasterPatch("first-updated", List.of())),
							IdentifiedModel.of("second", new MasterPatch("second-updated", List.of()))),
					BulkOperationMode.ALL_OR_NOTHING);

			assertThat(updated).hasSize(2);
			assertThat(scenario.masterStore.get("first").value()).isEqualTo("first-updated");
			assertThat(scenario.masterStore.get("second").value()).isEqualTo("second-updated");
			assertThat(scenario.transactionRunner.transactionCount()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("when deleting in bulk")
	final class WhenDeletingInBulk
	{
		@Test
		@DisplayName("it still uses per-item transactions in best-effort mode")
		void itStillUsesPerItemTransactionsInBestEffortMode()
		{
			var scenario = new TestScenario(List.of());
			scenario.masterStore.put("ok", new MasterModel("ok", List.of(), List.of()));
			scenario.masterStore.put("blocked", new MasterModel("blocked", List.of(), List.of()));
			scenario.masterDefinition.deletionPolicy = model ->
			{
				if ("blocked".equals(model.value()))
				{
					throw ResourceCannotBeDeletedException.withMessage("blocked");
				}
			};

			scenario.engine.deleteAllById(scenario.masterDefinition, List.of("ok", "blocked"),
					BulkOperationMode.BEST_EFFORT);

			assertThat(scenario.transactionRunner.transactionCount()).isEqualTo(2);
			assertThat(scenario.masterStore).doesNotContainKey("ok");
			assertThat(scenario.masterStore).containsKey("blocked");
		}
	}
}