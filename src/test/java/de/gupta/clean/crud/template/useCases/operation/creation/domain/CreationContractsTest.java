package de.gupta.clean.crud.template.useCases.operation.creation.domain;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.handler.CreationHandler;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreationContractsTest
{
	@Test
	void creationRequestCapturesPayloadAndSource()
	{
		var request = new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals("AAPL", request.payload().symbol());
		assertEquals(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT, request.source());
		assertEquals(OperationFamily.APPLICATION, request.family());
		assertEquals(OpenOrder.class, request.payloadType());
		assertEquals(Optional.empty(), request.correlationId());
	}

	@Test
	void creationRequestCanCarryCorrelationAndCausationMetadata()
	{
		var request = new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")));

		assertEquals("corr-1", request.correlationId().orElseThrow().value());
		assertEquals("cause-1", request.causationId().orElseThrow().value());
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

		assertEquals("order-1", context.domainId().orElseThrow());
		assertEquals(OperationSource.INTERNAL_COMMAND, context.source());
		assertEquals("SUBMITTED", context.afterModel().orElseThrow().status());
	}

	@Test
	void creationHandlerAppliesTypedPayloadToCreateInput()
	{
		CreationHandler<OrderCreate, OpenOrder> handler =
				payload -> new OrderCreate(payload.symbol(), payload.quantity());

		var create = handler.apply(new OpenOrder("AAPL", 100));

		assertEquals("AAPL", create.symbol());
		assertEquals(100, create.quantity());
	}

	@Test
	void resultCarriesCreateOutcome()
	{
		var created = new CreateResult<>(IdentifiedModel.of("order-1", new OrderState("SUBMITTED")));
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

		assertEquals("order-1", result.createdOrThrow().domainId());
		assertEquals("SUBMITTED", result.createdOrThrow().model().status());
	}

	@Test
	void requestRejectsMissingRequiredValues()
	{
		assertThrows(NullPointerException.class,
				() -> new CreationRequest<>(null, OperationSource.USER_INTENT));
		assertThrows(NullPointerException.class,
				() -> new CreationRequest<>(new OpenOrder("AAPL", 1), null));
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