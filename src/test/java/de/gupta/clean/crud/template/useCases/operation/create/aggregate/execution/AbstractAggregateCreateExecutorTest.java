package de.gupta.clean.crud.template.useCases.operation.create.aggregate.execution;

import de.gupta.clean.crud.template.domain.aggregate.builder.AggregateDefinitions;
import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateWorkflow;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractAggregateCreateExecutorTest
{
	@Test
	void create_persistsPlannedModel_andDispatchesPostCommitMutation()
	{
		var persistedModel = new AtomicReference<String>();
		var postCommitContext = new AtomicReference<PostCommitMutationContext<String, String>>();
		var definition = definitionWithoutRelationships(
				new AggregateMutationPort<>()
				{
					@Override
					public IdentifiedModel<String, String> create(final String domainModel)
					{
						persistedModel.set(domainModel);
						return IdentifiedModel.of("id-7", domainModel + ":persisted");
					}

					@Override
					public void put(final String domainId, final String domainModel)
					{
						throw new UnsupportedOperationException();
					}

					@Override
					public IdentifiedModel<String, String> update(final String domainId, final String domainModel)
					{
						throw new UnsupportedOperationException();
					}

					@Override
					public void delete(final String domainId)
					{
						throw new UnsupportedOperationException();
					}
				},
				postCommitContext::set);
		var executor = new TestAggregateCreateExecutor(definition, immediateEngine());

		var createdModel = executor.create(preparedAttempt("draft"));

		assertThat(createdModel).isEqualTo("draft:persisted");
		assertThat(persistedModel).hasValue("draft");
		assertThat(postCommitContext.get()).satisfies(context ->
		{
			assertThat(context.kind().name()).isEqualTo("CREATE");
			assertThat(context.domainId()).isEqualTo("id-7");
			assertThat(context.currentModel()).contains("draft:persisted");
			assertThat(context.previousModel()).isEmpty();
		});
	}

	private static AggregateDefinition<String, String, String, String, String> definitionWithoutRelationships(
			final AggregateMutationPort<String, String, String, String> mutationPort,
			final java.util.function.Consumer<PostCommitMutationContext<String, String>> postCommitMutation)
	{
		return AggregateDefinitions.<String, String, String, String, String>aggregateCrudDefinition()
		                           .mutationPort(mutationPort)
		                           .fetchPort(noopFetchPort())
		                           .createBuilder(createIdentity())
		                           .patcher((original, ignored) -> original)
		                           .responseBuilder(responseIdentity())
		                           .insertionPolicy(allowingInsertion())
		                           .patchPolicy(allowingPatch())
		                           .deletionPolicy(allowingDeletion())
		                           .securityPolicy(DomainSecurityPolicy.allowing())
		                           .duplicateDefinition(duplicatesNever())
		                           .postCommitMutation(postCommitMutation::accept)
		                           .build();
	}

	private static AggregateLifecycleEngine immediateEngine()
	{
		return new AggregateLifecycleEngine()
		{
			@Override
			public <Result> Result execute(
					final AggregateWorkflow<Result> workflow)
			{
				var result = workflow.inTransaction();
				workflow.afterTransaction(result);
				return result;
			}
		};
	}

	private static PreparedCreationAttempt<TestPayload, String> preparedAttempt(final String domainModel)
	{
		var request = new CreateOperationRequest<>(
				new TestPayload(domainModel),
				OperationRequestMetadata.source(OperationSource.USER_INTENT));
		return new PreparedCreationAttempt<>(
				request,
				RegisteredCreationHandler.of(TestPayload.class,
						ignored -> CreationPlan.of("aggregate.Task", domainModel)),
				CreateOperationContext.from(request),
				CreationPlan.of("aggregate.Task", domainModel));
	}

	private static AggregateFetchPort<String, String> noopFetchPort()
	{
		return new AggregateFetchPort<>()
		{
			@Override
			public Optional<IdentifiedModel<String, String>> findById(final String domainId)
			{
				return Optional.empty();
			}

			@Override
			public Collection<IdentifiedModel<String, String>> findByIds(final Set<String> domainIds)
			{
				return List.of();
			}

			@Override
			public Collection<IdentifiedModel<String, String>> findAll()
			{
				return List.of();
			}

			@Override
			public Slice<IdentifiedModel<String, String>> findAll(final Pageable pageable)
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	private static de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder<String, String> createIdentity()
	{
		return model -> model;
	}

	private static DomainResponseBuilder<String, String> responseIdentity()
	{
		return model -> model;
	}

	private static InsertionPolicy<String> allowingInsertion()
	{
		return _ ->
		{
		};
	}

	private static PatchPolicy<String> allowingPatch()
	{
		return (_, _) ->
		{
		};
	}

	private static DeletionPolicy<String> allowingDeletion()
	{
		return _ ->
		{
		};
	}

	private static DuplicateDefinition<String> duplicatesNever()
	{
		return (_, _) -> false;
	}

	@Test
	void create_failsFast_whenRelationshipsAreConfigured()
	{
		var createCalls = new AtomicReference<String>();
		var definition = definitionWithRelationship(new AggregateMutationPort<>()
		{
			@Override
			public IdentifiedModel<String, String> create(final String domainModel)
			{
				createCalls.set(domainModel);
				return IdentifiedModel.of("id-1", domainModel);
			}

			@Override
			public void put(final String domainId, final String domainModel)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public IdentifiedModel<String, String> update(final String domainId, final String domainModel)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public void delete(final String domainId)
			{
				throw new UnsupportedOperationException();
			}
		});
		var executor = new TestAggregateCreateExecutor(definition, immediateEngine());

		assertThatThrownBy(() -> executor.create(preparedAttempt("draft")))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("relationship definitions");
		assertThat(createCalls.get()).isNull();
	}

	private static AggregateDefinition<String, String, String, String, String> definitionWithRelationship(
			final AggregateMutationPort<String, String, String, String> mutationPort)
	{
		return AggregateDefinitions.<String, String, String, String, String>aggregateCrudDefinition()
		                           .mutationPort(mutationPort)
		                           .fetchPort(noopFetchPort())
		                           .createBuilder(createIdentity())
		                           .patcher((original, ignored) -> original)
		                           .responseBuilder(responseIdentity())
		                           .insertionPolicy(allowingInsertion())
		                           .patchPolicy(allowingPatch())
		                           .deletionPolicy(allowingDeletion())
		                           .securityPolicy(DomainSecurityPolicy.allowing())
		                           .duplicateDefinition(duplicatesNever())
		                           .relationshipDefinition(relationship())
		                           .build();
	}

	private static AggregateRelationshipDefinition<String, String, String, String, String, String, String, String> relationship()
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
				return Cardinality.ONE;
			}

			@Override
			public RelationshipKind relationshipKind()
			{
				return null;
			}

			@Override
			public LifecycleSemantics lifecycleSemantics()
			{
				return null;
			}

			@Override
			public ReconciliationStrategy reconciliationStrategy()
			{
				return ReconciliationStrategy.REPLACE;
			}

			@Override
			public AggregateDefinition<String, String, String, String, ?> satelliteDefinition()
			{
				return null;
			}

			@Override
			public AggregateMutationPort<String, String, String, String> satelliteMutationPort()
			{
				return null;
			}

			@Override
			public AggregateFetchPort<String, String> satelliteFetchPort()
			{
				return null;
			}

			@Override
			public SatelliteCreateInputResolver<String, Collection<SatelliteCreateIntent<String, String>>> createInputResolver()
			{
				return _ -> List.of();
			}

			@Override
			public SatellitePatchInputResolver<String, Collection<SatelliteMutationIntent<String, String, String>>> patchInputResolver()
			{
				return _ -> List.of();
			}

			@Override
			public SatelliteIdentityResolver<String, String, String> identityResolver()
			{
				return null;
			}

			@Override
			public SatelliteLinkStrategy<String, String, String, String> linkStrategy()
			{
				return null;
			}

			@Override
			public SatelliteHydrationStrategy<String, String, String, String> hydrationStrategy()
			{
				return null;
			}
		};
	}

	private record TestPayload(String value) implements CreateOperationPayload
	{
	}

	private static final class TestAggregateCreateExecutor
			extends AbstractAggregateCreateExecutor<TestPayload, String, String>
	{
		private TestAggregateCreateExecutor(
				final AggregateDefinition<String, String, ?, ?, ?> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}
}