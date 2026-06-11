package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;

public interface QuarantineReplayCodec
{
	QuarantineReplayEnvelope serialize(Object value);

	<T> T deserialize(QuarantineReplayEnvelope envelope, Class<T> expectedType);
}
