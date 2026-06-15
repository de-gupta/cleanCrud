package de.gupta.clean.crud.template.useCases.operation.create.aggregate.execution;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreateExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractAggregateCreateExecutor<DomainCreatePayload extends CreateOperationPayload, DomainId, DomainModel>
		implements CreateExecutor<DomainCreatePayload, DomainModel>
{
	private final AggregateDefinition<DomainId, DomainModel, ?, ?, ?> definition;
	private final AggregateLifecycle engine;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateMutationValidationSupport validationSupport;

	@Override
	public DomainModel create(final PreparedCreationAttempt<DomainCreatePayload, DomainModel> preparedAttempt)
	{
		Objects.requireNonNull(preparedAttempt, "preparedAttempt");
		throwIfRelationshipsConfigured();
		return engine.execute(
							 AggregateWorkflowBuilder.writeFlow(() -> persist(preparedAttempt.plan().domainModel()))
				                                     .startDurableProcesses(this::durableProcessStartRequests)
				                                     .afterTransaction(this::dispatchCreated)
				                                     .build())
		             .model();
	}

	private void throwIfRelationshipsConfigured()
	{
		if (!definitionGuard.satelliteRelationships(definition).isEmpty())
		{
			throw new UnsupportedOperationException(
					"Aggregate create execution with relationship definitions is not supported when the creation plan only carries a fully materialized domain model");
		}
	}

	private IdentifiedModel<DomainId, DomainModel> persist(final DomainModel domainModel)
	{
		validationSupport.validateSaveModels(definition, List.of(domainModel));
		return definition.mutationPort().create(domainModel);
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final IdentifiedModel<DomainId, DomainModel> createdModel)
	{
		return List.of();
	}

	private void dispatchCreated(final IdentifiedModel<DomainId, DomainModel> createdModel)
	{
		definition.postCommitMutation().accept(new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				createdModel.id(),
				Optional.of(createdModel.model()),
				Optional.empty()));
	}

	protected AbstractAggregateCreateExecutor(
			final AggregateDefinition<DomainId, DomainModel, ?, ?, ?> definition,
			final AggregateLifecycle engine)
	{
		this(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.validationSupport());
	}

	protected AbstractAggregateCreateExecutor(
			final AggregateDefinition<DomainId, DomainModel, ?, ?, ?> definition,
			final AggregateLifecycle engine,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateMutationValidationSupport validationSupport)
	{
		this.definition = Objects.requireNonNull(definition, "definition");
		this.engine = Objects.requireNonNull(engine, "engine");
		this.definitionGuard = Objects.requireNonNull(definitionGuard, "definitionGuard");
		this.validationSupport = Objects.requireNonNull(validationSupport, "validationSupport");
	}
}