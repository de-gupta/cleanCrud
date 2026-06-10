package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import java.util.Objects;

public record SerializedCreationPayload(
		String payloadType,
		String payloadJson)
{
	public SerializedCreationPayload
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(payloadJson, "payloadJson");
	}
}
