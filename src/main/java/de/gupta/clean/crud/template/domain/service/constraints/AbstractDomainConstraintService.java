package de.gupta.clean.crud.template.domain.service.constraints;

import de.gupta.aletheia.collection.crucible.Crucible;
import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateInsertionMessage;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class AbstractDomainConstraintService<DomainModel> implements DomainConstraintService<DomainModel>
{
	private final DuplicateInsertionMessage<DomainModel> duplicateInsertionMessage;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;
	private final Supplier<Collection<DomainModel>> existingModelsSupplier;

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
		return Unfolding.beckon(true)
						.metamorphose(_ -> duplicateConstraint(updatedModel, originalModel))
						.rescue(ConstraintResult.satisfied())
						.and(existingModelsConstraintService.mayThisResourceBeChangedTo(originalModel, updatedModel));
	}

	protected boolean enforceDuplicateConstraint()
	{
		return true;
	}

	private ConstraintResult duplicateConstraint(final DomainModel domainModel)
	{
		return Unfolding.beckon(isDuplicationConstraintRelevant(domainModel))
						.cleave(v -> v,
								ConstraintResult.violated(
										duplicateInsertionMessage.messageIfModelAlreadyExists(domainModel)),
								ConstraintResult.satisfied());
	}

	private ConstraintResult duplicateConstraint(final DomainModel updatedModel, final DomainModel originalModel)
	{
		return Unfolding.beckon(isDuplicationConstraintRelevant(updatedModel, originalModel))
						.cleave(v -> v,
								ConstraintResult.violated(
										duplicateInsertionMessage.messageIfModelAlreadyExists(updatedModel)),
								ConstraintResult.satisfied());
	}

	private boolean isDuplicationConstraintRelevant(final DomainModel updatedModel, final DomainModel originalModel)
	{
		return enforceDuplicateConstraint() && Crucible.kindle(existingModelsSupplier.get())
													   .banish(originalModel)
													   .harbors(updatedModel);
	}

	private boolean isDuplicationConstraintRelevant(final DomainModel domainModel)
	{
		return enforceDuplicateConstraint() && Crucible.kindle(existingModelsSupplier.get()).harbors(domainModel);
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