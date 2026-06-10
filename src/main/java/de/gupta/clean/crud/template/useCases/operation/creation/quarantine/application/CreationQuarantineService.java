package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;

import java.util.Collection;
import java.util.Optional;

public interface CreationQuarantineService extends CreationQuarantineRecorder
{
	@Override
	CreationQuarantineRequest record(CreationQuarantineSubmission submission);

	Optional<CreationQuarantineRecord> findById(CreationQuarantineId quarantineId);

	Collection<CreationQuarantineRecord> findOpen(int limit);

	CreationQuarantineRecord dismiss(CreationQuarantineId quarantineId);

	CreationQuarantineRecord replay(CreationQuarantineId quarantineId);
}
