package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceAlreadyExistsException;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.ResourceConstraintViolationException;
import de.gupta.clean.crud.template.domain.service.constraints.ConstraintResult;
import de.gupta.clean.crud.template.domain.service.constraints.ExistingModelsConstraintService;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;
import de.gupta.clean.crud.template.domain.service.existence.ResourceExistenceDetectionService;

public abstract class AbstractInsertionPolicy<DomainModel> implements InsertionPolicy<DomainModel>
{
	private final ResourceExistenceDetectionService<DomainModel> resourceExistenceDetectionService;
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;

	@Override
	public void validateInsertion(final DomainModel domainModel)
	{
		Unfolding.beckon(domainModel)
				 .discern(resourceExistenceDetectionService::existsByModel)
				 .interdict(ResourceAlreadyExistsException.forMessage(
						 duplicateInsertionMessage.messageIfModelAlreadyExists(domainModel)));

		Unfolding.beckon(domainModel)
				 .metamorphose(existingModelsConstraintService::mayThisResourceBeAdded)
				 .discern(ConstraintResult.Violated.class::isInstance)
				 .metamorphose(ConstraintResult.Violated.class::cast)
				 .interdict(v -> ResourceConstraintViolationException.forMessage(v.message()));
	}

	protected AbstractInsertionPolicy(
			final ResourceExistenceDetectionService<DomainModel> resourceExistenceDetectionService,
			final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage,
			final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService)
	{
		this.resourceExistenceDetectionService = resourceExistenceDetectionService;
		this.duplicateInsertionMessage = duplicateInsertionMessage;
		this.existingModelsConstraintService = existingModelsConstraintService;
	}
}