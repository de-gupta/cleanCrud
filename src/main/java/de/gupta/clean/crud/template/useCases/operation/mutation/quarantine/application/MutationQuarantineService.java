package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.util.Collection;
import java.util.Optional;

public interface MutationQuarantineService extends MutationQuarantineRecorder
{
	@Override
	MutationQuarantineRequest record(MutationQuarantineSubmission submission);

	Optional<MutationQuarantineRecord> findById(MutationQuarantineId quarantineId);

	Collection<MutationQuarantineRecord> findOpen(int limit);

	MutationQuarantineRecord dismiss(MutationQuarantineId quarantineId);

	MutationQuarantineRecord replay(MutationQuarantineId quarantineId);
}
