package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.MutationQuarantineService;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;

import java.util.Collection;

public abstract class AbstractMutationQuarantineApplicationController
		implements MutationQuarantineApplicationController
{
	private final MutationQuarantineService service;
	private final MutationQuarantineAccessPolicy accessPolicy;

	@Override
	public Collection<MutationQuarantineRecord> findOpen(final int limit)
	{
		accessPolicy.validateFindOpen();
		return service.findOpen(limit);
	}

	@Override
	public MutationQuarantineRecord findById(final MutationQuarantineId quarantineId)
	{
		accessPolicy.validateFindById(quarantineId);
		return service.findById(quarantineId)
		              .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId.value()));
	}

	@Override
	public MutationQuarantineRecord dismiss(final MutationQuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateDismiss(record);
		return service.dismiss(quarantineId);
	}

	@Override
	public MutationQuarantineRecord replay(final MutationQuarantineId quarantineId)
	{
		var record = findById(quarantineId);
		accessPolicy.validateReplay(record);
		return service.replay(quarantineId);
	}

	protected AbstractMutationQuarantineApplicationController(
			final MutationQuarantineService service,
			final MutationQuarantineAccessPolicy accessPolicy)
	{
		this.service = service;
		this.accessPolicy = accessPolicy;
	}
}
