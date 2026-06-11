package de.gupta.clean.crud.template.useCases.operation.quarantine.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineRecord;

public interface QuarantineAccessPolicy<P>
{
	static <P> QuarantineAccessPolicy<P> allowing()
	{
		return new QuarantineAccessPolicy<>()
		{
		};
	}

	default void validateFindOpen()
	{
	}

	default void validateFindById(final QuarantineId quarantineId)
	{
	}

	default void validateDismiss(final QuarantineRecord<P> record)
	{
	}

	default void validateReplay(final QuarantineRecord<P> record)
	{
	}
}
