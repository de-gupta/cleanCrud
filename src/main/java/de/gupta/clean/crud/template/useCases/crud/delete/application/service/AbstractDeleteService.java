package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;

import java.util.Collection;

public abstract class AbstractDeleteService<DomainID, DomainModel>
		implements DeleteService<DomainID>
{
	private final FetchPersistenceService<DomainID, DomainModel> fetchService;
	private final DeletePersistenceService<DomainID> persistenceService;
	private final DeletionPolicy<DomainModel> deletionPolicy;
	private final DomainSecurityPolicy<DomainModel> domainSecurityPolicy;

	@Override
	public void deleteById(final DomainID id)
	{
		validateDeletion(id);
		persistenceService.deleteById(id);
	}

	@Override
	public void deleteAllById(final Collection<DomainID> ids, final BulkOperationMode mode)
	{
		Collection<DomainID> validIds = switch (mode)
		{
			case ALL_OR_NOTHING ->
			{
				ids.forEach(this::validateDeletion);
				yield ids;
			}
			case BEST_EFFORT -> ids.stream()
								   .filter(this::isDeletionAllowed)
								   .toList();
		};

		persistenceService.deleteAllById(validIds);
	}

	private void validateDeletion(final DomainID id)
	{
		final var domainModel = fetchService.findById(id)
											.map(IdentifiedModel::model)
											.orElseThrow(() -> ResourceNotFoundException.withId(id));

		if (!domainSecurityPolicy.isAccessAllowed(domainModel))
			throw AccessDeniedException.withMessage("Access not allowed");

		deletionPolicy.validateDeletion(domainModel);
	}

	private boolean isDeletionAllowed(final DomainID id)
	{
		try
		{
			validateDeletion(id);
			return true;
		}
		catch (DomainException e)
		{
			return false;
		}
	}

	protected AbstractDeleteService(
			final FetchPersistenceService<DomainID, DomainModel> fetchService,
			final DeletePersistenceService<DomainID> persistenceService,
			final DeletionPolicy<DomainModel> deletionPolicy,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.fetchService = fetchService;
		this.persistenceService = persistenceService;
		this.deletionPolicy = deletionPolicy;
		this.domainSecurityPolicy = domainSecurityPolicy;
	}
}