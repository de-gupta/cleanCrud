package de.gupta.clean.crud.template.useCases.process.domain.definition;

import java.util.Objects;

public record DurableProcessDefinition<
		Trigger extends DurableProcessTrigger,
		Payload extends DurableProcessPayload>(
		String processType,
		Class<Trigger> triggerType,
		Class<Payload> payloadType)
{
	public static <Trigger extends DurableProcessTrigger, Payload extends DurableProcessPayload>
	DurableProcessDefinition<Trigger, Payload> of(
			final String processType,
			final Class<Trigger> triggerType,
			final Class<Payload> payloadType)
	{
		return new DurableProcessDefinition<>(processType, triggerType, payloadType);
	}

	public DurableProcessDefinition
	{
		if (processType == null || processType.isBlank())
		{
			throw new IllegalArgumentException("processType");
		}
		Objects.requireNonNull(triggerType, "triggerType");
		Objects.requireNonNull(payloadType, "payloadType");
	}

	public boolean supportsTrigger(final Class<?> candidate)
	{
		return triggerType.isAssignableFrom(candidate);
	}

	public boolean supportsPayload(final Class<?> candidate)
	{
		return payloadType.isAssignableFrom(candidate);
	}
}