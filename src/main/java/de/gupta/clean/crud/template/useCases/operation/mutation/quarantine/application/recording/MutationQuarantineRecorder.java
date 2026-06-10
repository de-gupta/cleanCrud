package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording;

import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;

@FunctionalInterface
public interface MutationQuarantineRecorder
{
	static MutationQuarantineRecorder noop()
	{
		return MutationQuarantineSubmission::quarantineRequest;
	}

	MutationQuarantineRequest record(MutationQuarantineSubmission submission);
}