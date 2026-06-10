package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineRecorder;

@FunctionalInterface
public interface AggregateLifecycleEngine
{
	<Result> Result execute(CrudWorkflow<Result> workflow);

	default MutationQuarantineRecorder mutationQuarantineRecorder()
	{
		return MutationQuarantineRecorder.noop();
	}
}
