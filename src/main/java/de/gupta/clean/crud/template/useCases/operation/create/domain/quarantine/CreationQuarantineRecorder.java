package de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine;

import java.util.Optional;

@FunctionalInterface
public interface CreationQuarantineRecorder
{
	static CreationQuarantineRecorder noop()
	{
		return _ -> Optional.empty();
	}

	Optional<String> record(CreationQuarantineRecordRequest request);
}