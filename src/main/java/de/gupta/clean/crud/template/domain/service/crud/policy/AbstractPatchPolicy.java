package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.ResourceConstraintViolationException;
import de.gupta.clean.crud.template.domain.service.constraints.ConstraintResult;
import de.gupta.clean.crud.template.domain.service.constraints.ExistingModelsConstraintService;

public abstract class AbstractPatchPolicy<DomainModel> implements PatchPolicy<DomainModel>
{
	private final ChangePolicy<DomainModel> changePolicy;
	private final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService;

	@Override
	public void validatePatchAttempt(final DomainModel originalModel, final DomainModel replacementModel)
	{
		changePolicy.validateChangeAttempt(originalModel, replacementModel);

		Unfolding.beckon(existingModelsConstraintService.mayThisResourceBeChangedTo(originalModel, replacementModel))
				 .discern(ConstraintResult::isViolated)
				 .metamorphose(ConstraintResult.Violated.class::cast)
				 .interdict(v -> ResourceConstraintViolationException.forMessage(v.message()));
	}

	protected AbstractPatchPolicy(
			final ChangePolicy<DomainModel> changePolicy,
			final ExistingModelsConstraintService<DomainModel> existingModelsConstraintService)
	{
		this.changePolicy = changePolicy;
		this.existingModelsConstraintService = existingModelsConstraintService;
	}
}