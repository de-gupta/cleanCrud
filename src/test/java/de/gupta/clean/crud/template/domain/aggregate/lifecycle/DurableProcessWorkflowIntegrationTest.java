package de.gupta.clean.crud.template.domain.aggregate.lifecycle;

import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.execution.AggregateWorkflowBuilder;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessExecutionNudge;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurableProcessWorkflowIntegrationTest
{
	@Test
	void enginePersistsDurableProcessStartsInsideTheTransactionBeforeAfterTransaction()
	{
		var transactionRunner = new RecordingTransactionRunner();
		var events = new ArrayList<String>();
		var startedRequests = new ArrayList<DurableProcessStartRequest<?, ?>>();
		var nudgedTaskIds = new ArrayList<DurableProcessTaskId>();
		var engine = DefaultAggregateLifecycle.withTransactionRunnerDispatcherStarterAndExecutionNudge(
				transactionRunner,
				new RecordingDispatcher(transactionRunner),
				new RecordingDurableProcessStarter(startedRequests, transactionRunner, events),
				new RecordingDurableProcessExecutionNudge(transactionRunner, nudgedTaskIds, events));
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1)));

		var result = engine.execute(
				AggregateWorkflowBuilder.writeFlow(() ->
										{
											events.add("business");
											return "order-1";
										})
				                        .startDurableProcesses(orderId -> List.of(new DurableProcessStartRequest<>(
												definition,
												new OrderSubmitted(orderId),
												new BrokerPayload("payload-" + orderId),
												new CorrelationId("corr-" + orderId),
												retryPolicy)))
				                        .afterTransaction(orderId -> events.add("after:" + orderId))
				                        .build());

		assertEquals("order-1", result);
		assertEquals(List.of("business", "start:submit-order", "after:order-1", "nudge:1"), events);
		assertEquals(1, startedRequests.size());
		assertEquals(1, nudgedTaskIds.size());
		assertTrue(transactionRunner.startedInsideTransaction);
		assertTrue(transactionRunner.afterTransactionOutsideTransaction);
		assertEquals(1, transactionRunner.transactionCount);
	}

	private record RecordingDurableProcessExecutionNudge(
			RecordingTransactionRunner transactionRunner,
			List<DurableProcessTaskId> nudgedTaskIds,
			List<String> events)
			implements DurableProcessExecutionNudge
	{
		@Override
		public void afterCommit(final Collection<DurableProcessTaskId> taskIds)
		{
			if (transactionRunner.inTransaction)
			{
				throw new AssertionError("Durable process nudge happened inside transaction");
			}
			nudgedTaskIds.addAll(taskIds);
			events.add("nudge:" + taskIds.size());
		}
	}

	private record OrderSubmitted(String orderId) implements DurableProcessTrigger
	{
	}

	private record BrokerPayload(String value) implements DurableProcessPayload
	{
	}

	private record RecordingDurableProcessStarter(List<DurableProcessStartRequest<?, ?>> startedRequests,
	                                              RecordingTransactionRunner transactionRunner, List<String> events)
			implements DurableProcessStarter
	{
		@Override
		public DurableProcessTaskId start(final DurableProcessStartRequest<?, ?> startRequest)
		{
			if (!transactionRunner.inTransaction)
			{
				throw new AssertionError("Durable process start happened outside transaction");
			}
			transactionRunner.startedInsideTransaction = true;
			startedRequests.add(startRequest);
			events.add("start:" + startRequest.definition().processType());
			return DurableProcessTaskId.random();
		}

	}

	private record RecordingDispatcher(RecordingTransactionRunner transactionRunner)
			implements PostCommitMutationDispatcher
	{
		@Override
		public <DomainId, DomainModel> void dispatch(
				final PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void dispatch(final Runnable action)
		{
			if (transactionRunner.inTransaction)
			{
				throw new AssertionError("afterTransaction dispatched inside transaction");
			}
			transactionRunner.afterTransactionOutsideTransaction = true;
			action.run();
		}

	}

	private static final class RecordingTransactionRunner implements PersistenceTransactionRunner
	{
		private boolean inTransaction;
		private boolean startedInsideTransaction;
		private boolean afterTransactionOutsideTransaction;
		private int transactionCount;

		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			transactionCount++;
			inTransaction = true;
			try
			{
				return action.get();
			}
			finally
			{
				inTransaction = false;
			}
		}
	}
}