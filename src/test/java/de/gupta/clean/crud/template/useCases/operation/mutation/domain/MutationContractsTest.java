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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutationContractsTest
{
	@Test
	void mutationRequestCapturesDomainIdPayloadAndSource()
	{
		var request = new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder("2026-06-09T13:30:00Z"),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals("order-1", request.domainId());
		assertEquals("2026-06-09T13:30:00Z", request.payload().acknowledgedAt());
		assertEquals(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT, request.source());
		assertEquals(OperationFamily.APPLICATION, request.family());
		assertEquals(AcknowledgeOrder.class, request.payloadType());
		assertEquals(Optional.empty(), request.correlationId());
		assertEquals(Optional.empty(), request.causationId());
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

		assertEquals("corr-1", request.correlationId().orElseThrow().value());
		assertEquals("cause-1", request.causationId().orElseThrow().value());
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

		assertEquals("order-1", context.domainId());
		assertEquals(OperationSource.INTERNAL_COMMAND, context.source());
		assertEquals(OperationFamily.APPLICATION, context.family());
		assertEquals(AcknowledgeOrder.class, context.payloadType());
		assertEquals("corr-1", context.correlationId().orElseThrow().value());
		assertEquals("cause-1", context.causationId().orElseThrow().value());
		assertEquals("SUBMITTED", context.beforeModel().orElseThrow().status());
		assertEquals("ACKNOWLEDGED", context.afterModel().orElseThrow().status());
	}

	@Test
	void mutationHandlerAppliesTypedPayloadToCurrentModel()
	{
		MutationHandler<OrderState, ApplyFill> handler =
				(currentModel, payload) -> new OrderState(currentModel.status() + ":" + payload.fillQuantity());

		var updated = handler.apply(new OrderState("PARTIALLY_FILLED"), new ApplyFill(10));

		assertEquals("PARTIALLY_FILLED:10", updated.status());
	}

	@Test
	void requestRejectsMissingRequiredValues()
	{
		assertThrows(NullPointerException.class,
				() -> new MutationRequest<>("order-1", null, OperationSource.USER_INTENT));
		assertThrows(NullPointerException.class,
				() -> new MutationRequest<>("order-1", new ApplyFill(1), null));
	}

	@Test
	void contextRejectsMissingRequiredValues()
	{
		assertThrows(NullPointerException.class,
				() -> new MutationContext<>("order-1", OperationSource.USER_INTENT, OperationFamily.APPLICATION,
						ApplyFill.class, Optional.empty(), Optional.empty(), null, Optional.empty()));
		assertThrows(NullPointerException.class,
				() -> new MutationContext<>("order-1", OperationSource.USER_INTENT, OperationFamily.APPLICATION,
						ApplyFill.class, Optional.empty(), Optional.empty(), Optional.empty(), null));
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