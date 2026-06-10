package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

public interface CreationQuarantinePayloadCodec
{
	SerializedCreationPayload serialize(ApplicationOperationPayload payload);

	ApplicationOperationPayload deserialize(SerializedCreationPayload payload);
}
