package de.gupta.clean.crud.template.domain.service.constraints;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;
import de.gupta.clean.crud.template.domain.service.existence.ResourceExistenceDetectionService;

public abstract class AbstractDomainConstraintService<DomainModel> implements DomainConstraintService<DomainModel>
{
	private final ResourceExistenceDetectionService<DomainModel> existenceDetectionService;
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;

	@Override
	public ConstraintResult validateForInsertion(final DomainModel domainModel)
	{
		return Unfolding.beckon(domainModel)
						.metamorphose(this::duplicateConstraint)
						.summon()
						.and(existingModelsConstraintService.mayThisResourceBeAdded(domainModel));
	}

	@Override
	public ConstraintResult validateForUpdate(final DomainModel originalModel, final DomainModel updatedModel)
	{
		return Unfolding.beckon(updatedModel)
						.metamorphose(this::duplicateConstraint)
						.summon()
						.and(existingModelsConstraintService.mayThisResourceBeChangedTo(originalModel, updatedModel));
	}

	private ConstraintResult duplicateConstraint(final DomainModel domainModel)
	{
		return Unfolding.beckon(domainModel)
						.cleave(existenceDetectionService::existsByModel, ConstraintResult.violated(
										duplicateInsertionMessage.messageIfModelAlreadyExists(domainModel)),
								ConstraintResult.satisfied());
	}

	protected AbstractDomainConstraintService(
			final ResourceExistenceDetectionService<DomainModel> existenceDetectionService,
			final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage,
			final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService)
	{
		this.existenceDetectionService = existenceDetectionService;
		this.duplicateInsertionMessage = duplicateInsertionMessage;
		this.existingModelsConstraintService = existingModelsConstraintService;
	}
}