package de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.policy;

import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;

public interface MutationQuarantineAccessPolicy
{
	static MutationQuarantineAccessPolicy allowing()
	{
		return new MutationQuarantineAccessPolicy()
		{
		};
	}

	default void validateFindOpen()
	{
	}

	default void validateFindById(final MutationQuarantineId quarantineId)
	{
	}

	default void validateDismiss(final MutationQuarantineRecord record)
	{
	}

	default void validateReplay(final MutationQuarantineRecord record)
	{
	}
}
