package de.gupta.clean.crud.template.useCases.mutation.domain;

import de.gupta.clean.crud.template.useCases.mutation.domain.handler.MutationHandler;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
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
				MutationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals("order-1", request.domainId());
		assertEquals("2026-06-09T13:30:00Z", request.payload().acknowledgedAt());
		assertEquals(MutationSource.AUTHORITATIVE_EXTERNAL_EVENT, request.source());
	}

	@Test
	void mutationContextCapturesSourceAndBeforeAfterModels()
	{
		var context = new MutationContext<>(
				"order-1",
				MutationSource.INTERNAL_COMMAND,
				Optional.of(new OrderState("SUBMITTED")),
				Optional.of(new OrderState("ACKNOWLEDGED")));

		assertEquals("order-1", context.domainId());
		assertEquals(MutationSource.INTERNAL_COMMAND, context.source());
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
				() -> new MutationRequest<>("order-1", null, MutationSource.USER_INTENT));
		assertThrows(NullPointerException.class,
				() -> new MutationRequest<>("order-1", new ApplyFill(1), null));
	}

	@Test
	void contextRejectsMissingRequiredValues()
	{
		assertThrows(NullPointerException.class,
				() -> new MutationContext<>("order-1", MutationSource.USER_INTENT, null, Optional.empty()));
		assertThrows(NullPointerException.class,
				() -> new MutationContext<>("order-1", MutationSource.USER_INTENT, Optional.empty(), null));
	}

	private record OrderState(String status)
	{
	}

	private record AcknowledgeOrder(String acknowledgedAt) implements ApplicationMutationPayload
	{
	}

	private record ApplyFill(int fillQuantity) implements ApplicationMutationPayload
	{
	}
}
