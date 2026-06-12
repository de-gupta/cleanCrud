package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler.AggregateCreationHandler;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreationContractsTest
{
	@Test
	void creationRequestCapturesPayloadAndSource()
	{
		var request = new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertThat(request.payload().symbol())
				.as("request should capture payload symbol")
				.isEqualTo("AAPL");
		assertThat(request.source())
				.as("request should capture operation source")
				.isEqualTo(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
		assertThat(request.family())
				.as("request family should default to APPLICATION")
				.isEqualTo(OperationFamily.APPLICATION);
		assertThat(request.payloadType())
				.as("request should capture payload type")
				.isEqualTo(OpenOrder.class);
		assertThat(request.correlationId())
				.as("request without correlation id should be empty")
				.isEmpty();
	}

	@Test
	void creationRequestCanCarryCorrelationAndCausationMetadata()
	{
		var request = new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")));

		assertThat(request.correlationId().orElseThrow().value())
				.as("request should carry correlation id value")
				.isEqualTo("corr-1");
		assertThat(request.causationId().orElseThrow().value())
				.as("request should carry causation id value")
				.isEqualTo("cause-1");
	}

	@Test
	void creationContextCapturesCreatedAggregateMetadata()
	{
		var context = new CreationContext<String, OrderState>(
				Optional.empty(),
				OperationSource.INTERNAL_COMMAND,
				OperationFamily.APPLICATION,
				OpenOrder.class,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")),
				Optional.empty(),
				Optional.of(new OrderState("SUBMITTED")))
				.withCreated("order-1", new OrderState("SUBMITTED"));

		assertThat(context.domainId().orElseThrow())
				.as("context should carry the created domain id")
				.isEqualTo("order-1");
		assertThat(context.source())
				.as("context should preserve the operation source")
				.isEqualTo(OperationSource.INTERNAL_COMMAND);
		assertThat(context.afterModel().orElseThrow().status())
				.as("context should carry the after model state")
				.isEqualTo("SUBMITTED");
	}

	@Test
	void creationHandlerAppliesTypedPayloadToCreateInput()
	{
		AggregateCreationHandler<OrderCreate, OpenOrder> handler =
				payload -> AggregateCreationPlan.rootOnly(new OrderCreate(payload.symbol(), payload.quantity()));

		var create = handler.apply(new OpenOrder("AAPL", 100)).rootCreate().orElseThrow();

		assertThat(create.symbol())
				.as("handler should map payload symbol to create input")
				.isEqualTo("AAPL");
		assertThat(create.quantity())
				.as("handler should map payload quantity to create input")
				.isEqualTo(100);
	}

	@Test
	void resultCarriesCreateOutcome()
	{
		var created = CreateResult.of("order-1", new OrderState("SUBMITTED"));
		var result = CreationResult.created(
				new CreationContext<>(
						Optional.of("order-1"),
						OperationSource.INTERNAL_COMMAND,
						OperationFamily.APPLICATION,
						OpenOrder.class,
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.of(new OrderState("SUBMITTED"))),
				CreationPolicyDecision.allow(),
				created);

		assertThat(result.createdOrThrow().domainId())
				.as("result should carry the created domain id")
				.isEqualTo("order-1");
		assertThat(result.createdOrThrow().model().status())
				.as("result should carry the created model state")
				.isEqualTo("SUBMITTED");
	}

	@Test
	void requestRejectsMissingRequiredValues()
	{
		assertThatThrownBy(() -> new CreationRequest<>(null, OperationSource.USER_INTENT))
				.as("null payload should throw")
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new CreationRequest<>(new OpenOrder("AAPL", 1), null))
				.as("null source should throw")
				.isInstanceOf(NullPointerException.class);
	}

	private record OrderState(String status)
	{
	}

	private record OrderCreate(String symbol, int quantity)
	{
	}

	private record OpenOrder(String symbol, int quantity) implements ApplicationOperationPayload
	{
	}
}