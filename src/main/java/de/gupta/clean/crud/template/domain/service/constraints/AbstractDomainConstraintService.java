package de.gupta.clean.crud.template.domain.service.constraints;

import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class AbstractDomainConstraintService<DomainModel> implements DomainConstraintService<DomainModel>
{
	private final DuplicateDefinition<DomainModel> duplicateDefinition;
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;
	private final Supplier<Collection<DomainModel>> existingModelsSupplier;

	@Override
	public ConstraintResult validateForInsertion(final DomainModel domainModel)
	{
		return validateDuplicateConstraint(domainModel, _ -> false)
				.and(existingModelsConstraintService.mayThisResourceBeAdded(domainModel));
	}

	@Override
	public ConstraintResult validateForUpdate(final DomainModel originalModel, final DomainModel updatedModel)
	{
		return validateDuplicateConstraint(updatedModel,
				existingModel -> duplicateDefinition.areDuplicates(existingModel, originalModel))
				.and(existingModelsConstraintService.mayThisResourceBeChangedTo(originalModel, updatedModel));
	}

	protected boolean enforceDuplicateConstraint()
	{
		return true;
	}

	private ConstraintResult validateDuplicateConstraint(final DomainModel candidate,
														 final DuplicateExemption<DomainModel> exemption)
	{
		if (!enforceDuplicateConstraint())
		{
			return ConstraintResult.satisfied();
		}
		var existingModels = existingModelsSupplier.get();
		return existingModels.stream()
							 .filter(existingModel -> !exemption.exempt(existingModel))
							 .anyMatch(existingModel -> duplicateDefinition.areDuplicates(existingModel, candidate))
				? ConstraintResult.violated(duplicateInsertionMessage.messageIfModelAlreadyExists(candidate))
				: ConstraintResult.satisfied();
	}

	protected AbstractDomainConstraintService(
			final DuplicateDefinition<DomainModel> duplicateDefinition,
			final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage,
			final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService,
			final Supplier<Collection<DomainModel>> existingModelsSupplier)
	{
		this.duplicateDefinition = duplicateDefinition;
		this.duplicateInsertionMessage = duplicateInsertionMessage;
		this.existingModelsConstraintService = existingModelsConstraintService;
		this.existingModelsSupplier = existingModelsSupplier;
	}

	@FunctionalInterface
	private interface DuplicateExemption<DomainModel>
	{
		boolean exempt(DomainModel existingModel);
	}
}