package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineRecorder;

@FunctionalInterface
public interface AggregateLifecycleEngine
{
	<Result> Result execute(CrudWorkflow<Result> workflow);

	default MutationQuarantineRecorder mutationQuarantineRecorder()
	{
		return MutationQuarantineRecorder.noop();
	}

	default CreationQuarantineRecorder creationQuarantineRecorder()
	{
		return CreationQuarantineRecorder.noop();
	}
}