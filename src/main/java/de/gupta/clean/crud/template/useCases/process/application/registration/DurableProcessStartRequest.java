package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;

import java.util.Objects;

public record DurableProcessStartRequest<
		Trigger extends DurableProcessTrigger,
		Payload extends DurableProcessPayload>(
		DurableProcessDefinition<Trigger, Payload> definition,
		Trigger trigger,
		Payload payload,
		CorrelationId correlationId,
		RetryPolicy retryPolicy)
{
	public DurableProcessStartRequest
	{
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(trigger, "trigger");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(retryPolicy, "retryPolicy");
		if (!definition.supportsTrigger(trigger.getClass()))
		{
			throw new IllegalArgumentException("trigger");
		}
		if (!definition.supportsPayload(payload.getClass()))
		{
			throw new IllegalArgumentException("payload");
		}
	}
}
