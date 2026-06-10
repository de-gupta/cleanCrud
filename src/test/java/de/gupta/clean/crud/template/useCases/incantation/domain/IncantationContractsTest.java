package de.gupta.clean.crud.template.useCases.incantation.domain;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.IncantationHandler;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.*;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyDecision;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncantationContractsTest
{
	@Test
	void incantationRequestCapturesPayloadAndSource()
	{
		var request = new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals("AAPL", request.payload().symbol());
		assertEquals(IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT, request.source());
		assertEquals(IncantationFamily.APPLICATION, request.family());
		assertEquals(OpenOrder.class, request.payloadType());
		assertEquals(Optional.empty(), request.correlationId());
	}

	@Test
	void incantationRequestCanCarryCorrelationAndCausationMetadata()
	{
		var request = new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new IncantationCorrelationId("corr-1")),
				Optional.of(new IncantationCausationId("cause-1")));

		assertEquals("corr-1", request.correlationId().orElseThrow().value());
		assertEquals("cause-1", request.causationId().orElseThrow().value());
	}

	@Test
	void incantationContextCapturesCreatedAggregateMetadata()
	{
		var context = new IncantationContext<String, OrderState>(
				Optional.empty(),
				IncantationSource.INTERNAL_COMMAND,
				IncantationFamily.APPLICATION,
				OpenOrder.class,
				Optional.of(new IncantationCorrelationId("corr-1")),
				Optional.of(new IncantationCausationId("cause-1")),
				Optional.empty(),
				Optional.of(new OrderState("SUBMITTED")))
				.withCreated("order-1", new OrderState("SUBMITTED"));

		assertEquals("order-1", context.domainId().orElseThrow());
		assertEquals(IncantationSource.INTERNAL_COMMAND, context.source());
		assertEquals("SUBMITTED", context.afterModel().orElseThrow().status());
	}

	@Test
	void incantationHandlerAppliesTypedPayloadToCreateInput()
	{
		IncantationHandler<OrderCreate, OpenOrder> handler =
				payload -> new OrderCreate(payload.symbol(), payload.quantity());

		var create = handler.apply(new OpenOrder("AAPL", 100));

		assertEquals("AAPL", create.symbol());
		assertEquals(100, create.quantity());
	}

	@Test
	void resultCarriesCreateOutcome()
	{
		var created = new CreateResult<>(IdentifiedModel.of("order-1", new OrderState("SUBMITTED")));
		var result = IncantationResult.created(
				new IncantationContext<>(
						Optional.of("order-1"),
						IncantationSource.INTERNAL_COMMAND,
						IncantationFamily.APPLICATION,
						OpenOrder.class,
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.of(new OrderState("SUBMITTED"))),
				IncantationPolicyDecision.allow(),
				created);

		assertEquals("order-1", result.createdOrThrow().domainId());
		assertEquals("SUBMITTED", result.createdOrThrow().model().status());
	}

	@Test
	void requestRejectsMissingRequiredValues()
	{
		assertThrows(NullPointerException.class,
				() -> new IncantationRequest<>(null, IncantationSource.USER_INTENT));
		assertThrows(NullPointerException.class,
				() -> new IncantationRequest<>(new OpenOrder("AAPL", 1), null));
	}

	private record OrderState(String status)
	{
	}

	private record OrderCreate(String symbol, int quantity)
	{
	}

	private record OpenOrder(String symbol, int quantity) implements ApplicationIncantationPayload
	{
	}
}
