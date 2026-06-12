package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineRecord;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.policy.QuarantineAccessPolicy;

import java.util.Collection;

public abstract class AbstractQuarantineApplicationController<P extends PayloadReplayInputs>
		implements QuarantineApplicationController<P>
{
	private final QuarantineService<P> service;
	private final QuarantineAccessPolicy<P> accessPolicy;

	@Override
	public Collection<QuarantineRecord<P>> findOpen(final int limit)
	{
		accessPolicy.validateFindOpen();
		return service.findOpen(limit);
	}

	@Override
	public QuarantineRecord<P> findById(final QuarantineId quarantineId)
	{
		accessPolicy.validateFindById(quarantineId);
		return service.findById(quarantineId)
		              .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId.value()));
	}

	@Override
	public QuarantineRecord<P> dismiss(final QuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateDismiss(record);
		return service.dismiss(quarantineId);
	}

	@Override
	public QuarantineRecord<P> replay(final QuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateReplay(record);
		return service.replay(quarantineId);
	}

	protected AbstractQuarantineApplicationController(
			final QuarantineService<P> service,
			final QuarantineAccessPolicy<P> accessPolicy)
	{
		this.service = service;
		this.accessPolicy = accessPolicy;
	}
}