package de.gupta.clean.crud.template.domain.service.constraints;

import de.gupta.aletheia.collection.crucible.Crucible;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractDomainConstraintService<DomainModel> implements DomainConstraintService<DomainModel>
{
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;
	private final Supplier<Collection<DomainModel>> existingModelsSupplier;

	@Override
	public ConstraintResult validateForInsertion(final DomainModel domainModel)
	{
		return validateDuplicateConstraint(domainModel)
				.and(existingModelsConstraintService.mayThisResourceBeAdded(domainModel));
	}

	@Override
	public ConstraintResult validateForUpdate(final DomainModel originalModel, final DomainModel updatedModel)
	{
		return validateDuplicateConstraintForUpdate(updatedModel, originalModel)
				.and(existingModelsConstraintService.mayThisResourceBeChangedTo(originalModel, updatedModel));
	}

	protected boolean enforceDuplicateConstraint()
	{
		return true;
	}

	private ConstraintResult validateDuplicateConstraint(final DomainModel domainModel)
	{
		return validateConstraintIfApplicable(domainModel,
				crucible -> crucible.harbors(domainModel));
	}

	private ConstraintResult validateDuplicateConstraintForUpdate(final DomainModel updatedModel,
																  final DomainModel originalModel)
	{
		return validateConstraintIfApplicable(updatedModel,
				crucible -> crucible.banish(originalModel).harbors(updatedModel));
	}

	private ConstraintResult validateConstraintIfApplicable(final DomainModel domainModel,
															final Predicate<Crucible<DomainModel>> constraintCheck)
	{
		return enforceDuplicateConstraint() && constraintCheck.test(Crucible.kindle(existingModelsSupplier.get()))
				? ConstraintResult.violated(duplicateInsertionMessage.messageIfModelAlreadyExists(domainModel))
				: ConstraintResult.satisfied();
	}

	protected AbstractDomainConstraintService(
			final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage,
			final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService,
			final Supplier<Collection<DomainModel>> existingModelsSupplier)
	{
		this.duplicateInsertionMessage = duplicateInsertionMessage;
		this.existingModelsConstraintService = existingModelsConstraintService;
		this.existingModelsSupplier = existingModelsSupplier;
	}
}