package de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.application.recording;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.CreationQuarantineRequest;

@FunctionalInterface
public interface CreationQuarantineRecorder
{
	static CreationQuarantineRecorder noop()
	{
		return CreationQuarantineSubmission::quarantineRequest;
	}

	CreationQuarantineRequest record(CreationQuarantineSubmission submission);
}