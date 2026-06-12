package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.policy;

import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineRecord;

public interface QuarantineAccessPolicy<P extends PayloadReplayInputs>
{
	static <P extends PayloadReplayInputs> QuarantineAccessPolicy<P> allowing()
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