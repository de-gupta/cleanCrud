package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceAlreadyExistsException;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.ResourceConstraintViolationException;
import de.gupta.clean.crud.template.domain.service.constraints.DomainConstraintService;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;
import de.gupta.clean.crud.template.domain.service.existence.ResourceExistenceDetectionService;

import java.util.Optional;

public abstract class AbstractInsertionPolicy<DomainModel> implements InsertionPolicy<DomainModel>
{
	private final ResourceExistenceDetectionService<DomainModel> resourceExistenceDetectionService;
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final DomainConstraintService<DomainModel> domainConstraintService;

	@Override
	public void validateInsertion(final DomainModel domainModel)
	{
		Optional.of(domainModel).filter(resourceExistenceDetectionService::existsByModel).ifPresent(model ->
		{
			throw ResourceAlreadyExistsException.withMessage(
					duplicateInsertionMessage.messageIfModelAlreadyExists(model));
		});

		domainConstraintService.mayThisResourceBeAdded(domainModel).message().ifPresent(message ->
		{
			throw ResourceConstraintViolationException.withMessage(message);
		});
	}

	protected AbstractInsertionPolicy(
			final ResourceExistenceDetectionService<DomainModel> resourceExistenceDetectionService,
			final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage,
			final DomainConstraintService<DomainModel> domainConstraintService)
	{
		this.resourceExistenceDetectionService = resourceExistenceDetectionService;
		this.duplicateInsertionMessage = duplicateInsertionMessage;
		this.domainConstraintService = domainConstraintService;
	}
}