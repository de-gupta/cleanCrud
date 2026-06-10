package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;

public interface CreationQuarantineAccessPolicy
{
	static CreationQuarantineAccessPolicy allowing()
	{
		return new CreationQuarantineAccessPolicy()
		{
		};
	}

	default void validateFindOpen()
	{
	}

	default void validateFindById(final CreationQuarantineId quarantineId)
	{
	}

	default void validateDismiss(final CreationQuarantineRecord record)
	{
	}

	default void validateReplay(final CreationQuarantineRecord record)
	{
	}
}
