package de.gupta.clean.crud.template.useCases.operation.quarantine.application;

import java.time.Instant;

public interface QuarantineRecorder<Submission, Record, PersistedRequest>
{
	Record buildRecord(Submission submission, Instant now);

	PersistedRequest persistedRequest(Submission submission, Record savedRecord);
}
