package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.policy.CreationQuarantineAccessPolicy;

import java.util.Collection;

public abstract class AbstractCreationQuarantineApplicationController
		implements CreationQuarantineApplicationController
{
	private final CreationQuarantineService service;
	private final CreationQuarantineAccessPolicy accessPolicy;

	@Override
	public Collection<CreationQuarantineRecord> findOpen(final int limit)
	{
		accessPolicy.validateFindOpen();
		return service.findOpen(limit);
	}

	@Override
	public CreationQuarantineRecord findById(final CreationQuarantineId quarantineId)
	{
		accessPolicy.validateFindById(quarantineId);
		return service.findById(quarantineId)
		              .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId.value()));
	}

	@Override
	public CreationQuarantineRecord dismiss(final CreationQuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateDismiss(record);
		return service.dismiss(quarantineId);
	}

	@Override
	public CreationQuarantineRecord replay(final CreationQuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateReplay(record);
		return service.replay(quarantineId);
	}

	protected AbstractCreationQuarantineApplicationController(
			final CreationQuarantineService service,
			final CreationQuarantineAccessPolicy accessPolicy)
	{
		this.service = service;
		this.accessPolicy = accessPolicy;
	}
}
