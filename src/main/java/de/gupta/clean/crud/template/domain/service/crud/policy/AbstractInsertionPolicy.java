package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.ResourceConstraintViolationException;
import de.gupta.clean.crud.template.domain.service.constraints.ConstraintResult;
import de.gupta.clean.crud.template.domain.service.constraints.DomainConstraintService;

public abstract class AbstractInsertionPolicy<DomainModel> implements InsertionPolicy<DomainModel>
{
	private final DomainConstraintService<DomainModel> domainConstraintService;

	@Override
	public void validateInsertion(final DomainModel domainModel)
	{
		Unfolding.beckon(domainConstraintService.validateForInsertion(domainModel))
				 .evolve(ConstraintResult.Violated.class::isInstance, ConstraintResult.Violated.class::cast)
				 .interdict(v -> ResourceConstraintViolationException.forMessage(v.message()));
	}

	protected AbstractInsertionPolicy(final DomainConstraintService<DomainModel> domainConstraintService)
	{
		this.domainConstraintService = domainConstraintService;
	}
}