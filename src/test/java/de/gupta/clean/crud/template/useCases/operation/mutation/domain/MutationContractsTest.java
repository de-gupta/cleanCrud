package de.gupta.clean.crud.template.useCases.operation.mutation.domain;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler.MutationHandler;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MutationContractsTest
{
	@Test
	void mutationRequestCapturesDomainIdPayloadAndSource()
	{
		var request = new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder("2026-06-09T13:30:00Z"),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertThat(request.domainId())
				.as("request should carry the domain id")
				.isEqualTo("order-1");
		assertThat(request.payload().acknowledgedAt())
				.as("request should carry the typed payload")
				.isEqualTo("2026-06-09T13:30:00Z");
		assertThat(request.source())
				.as("request should capture the operation source")
				.isEqualTo(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
		assertThat(request.family())
				.as("request family should default to APPLICATION")
				.isEqualTo(OperationFamily.APPLICATION);
		assertThat(request.payloadType())
				.as("request should capture the payload type")
				.isEqualTo(AcknowledgeOrder.class);
		assertThat(request.correlationId())
				.as("request without correlation id should be empty")
				.isEmpty();
		assertThat(request.causationId())
				.as("request without causation id should be empty")
				.isEmpty();
	}

	@Test
	void mutationRequestCanCarryCorrelationAndCausationMetadata()
	{
		var request = new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder("2026-06-09T13:30:00Z"),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")));

		assertThat(request.correlationId().orElseThrow().value())
				.as("request should carry the correlation id value")
				.isEqualTo("corr-1");
		assertThat(request.causationId().orElseThrow().value())
				.as("request should carry the causation id value")
				.isEqualTo("cause-1");
	}

	@Test
	void mutationContextCapturesSourceAndBeforeAfterModels()
	{
		var context = new MutationContext<>(
				"order-1",
				OperationSource.INTERNAL_COMMAND,
				OperationFamily.APPLICATION,
				AcknowledgeOrder.class,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")),
				Optional.of(new OrderState("SUBMITTED")),
				Optional.of(new OrderState("ACKNOWLEDGED")));

		assertThat(context.domainId())
				.as("context should carry the domain id")
				.isEqualTo("order-1");
		assertThat(context.source())
				.as("context should preserve the operation source")
				.isEqualTo(OperationSource.INTERNAL_COMMAND);
		assertThat(context.family())
				.as("context should preserve the operation family")
				.isEqualTo(OperationFamily.APPLICATION);
		assertThat(context.payloadType())
				.as("context should carry the payload type")
				.isEqualTo(AcknowledgeOrder.class);
		assertThat(context.correlationId().orElseThrow().value())
				.as("context should carry the correlation id value")
				.isEqualTo("corr-1");
		assertThat(context.causationId().orElseThrow().value())
				.as("context should carry the causation id value")
				.isEqualTo("cause-1");
		assertThat(context.beforeModel().orElseThrow().status())
				.as("context should carry the before model state")
				.isEqualTo("SUBMITTED");
		assertThat(context.afterModel().orElseThrow().status())
				.as("context should carry the after model state")
				.isEqualTo("ACKNOWLEDGED");
	}

	@Test
	void mutationHandlerAppliesTypedPayloadToCurrentModel()
	{
		MutationHandler<OrderState, ApplyFill> handler =
				(currentModel, payload) -> new OrderState(currentModel.status() + ":" + payload.fillQuantity());

		var updated = handler.apply(new OrderState("PARTIALLY_FILLED"), new ApplyFill(10));

		assertThat(updated.status())
				.as("handler should produce updated model from current and payload")
				.isEqualTo("PARTIALLY_FILLED:10");
	}

	@Test
	void requestRejectsMissingRequiredValues()
	{
		assertThatThrownBy(() -> new MutationRequest<>("order-1", null, OperationSource.USER_INTENT))
				.as("null payload should throw")
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new MutationRequest<>("order-1", new ApplyFill(1), null))
				.as("null source should throw")
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void contextRejectsMissingRequiredValues()
	{
		assertThatThrownBy(
				() -> new MutationContext<>("order-1", OperationSource.USER_INTENT, OperationFamily.APPLICATION,
						ApplyFill.class, Optional.empty(), Optional.empty(), null, Optional.empty()))
				.as("null beforeModel should throw")
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(
				() -> new MutationContext<>("order-1", OperationSource.USER_INTENT, OperationFamily.APPLICATION,
						ApplyFill.class, Optional.empty(), Optional.empty(), Optional.empty(), null))
				.as("null afterModel should throw")
				.isInstanceOf(NullPointerException.class);
	}

	private record OrderState(String status)
	{
	}

	private record AcknowledgeOrder(String acknowledgedAt) implements ApplicationOperationPayload
	{
	}

	private record ApplyFill(int fillQuantity) implements ApplicationOperationPayload
	{
	}
}