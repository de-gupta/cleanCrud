package de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording;

import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.quarantine.MutationQuarantineRequest;

@FunctionalInterface
public interface MutationQuarantineRecorder
{
	static MutationQuarantineRecorder noop()
	{
		return MutationQuarantineSubmission::quarantineRequest;
	}

	MutationQuarantineRequest record(MutationQuarantineSubmission submission);
}