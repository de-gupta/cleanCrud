package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayEnvelope;

public interface QuarantineReplayCodec
{
	QuarantineReplayEnvelope serialize(Object value);

	<T> T deserialize(QuarantineReplayEnvelope envelope, Class<T> expectedType);
}