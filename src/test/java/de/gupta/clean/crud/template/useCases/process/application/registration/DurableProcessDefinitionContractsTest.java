package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurableProcessDefinitionContractsTest
{
	@Test
	void definitionCapturesProcessTypeAndSupportedTypes()
	{
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);

		assertEquals("submit-order", definition.processType());
		assertTrue(definition.supportsTrigger(OrderSubmitted.class));
		assertTrue(definition.supportsPayload(BrokerPayload.class));
	}

	@Test
	void startRequestBindsDefinitionTriggerPayloadAndPolicy()
	{
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);
		var retryPolicy = new RetryPolicy(5, BackoffPolicy.fixed(Duration.ofSeconds(1)));
		var request = new DurableProcessStartRequest<>(
				definition,
				new OrderSubmitted("order-1"),
				new BrokerPayload("payload-1"),
				new CorrelationId("corr-1"),
				retryPolicy);

		assertEquals(definition, request.definition());
		assertEquals("order-1", request.trigger().orderId());
		assertEquals("payload-1", request.payload().value());
		assertEquals(retryPolicy, request.retryPolicy());
	}

	private record OrderSubmitted(String orderId) implements DurableProcessTrigger
	{
	}

	private record BrokerPayload(String value) implements DurableProcessPayload
	{
	}
}
