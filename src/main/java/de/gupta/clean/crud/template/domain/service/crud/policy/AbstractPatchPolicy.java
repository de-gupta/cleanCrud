package de.gupta.clean.crud.template.domain.service.crud.policy;

import java.util.Optional;

public abstract class AbstractPatchPolicy<DomainModel> implements PatchPolicy<DomainModel>
{
	private final ChangePolicy<DomainModel> changePolicy;
	private final InsertionPolicy<DomainModel> insertionPolicy;

	@Override
	public void validatePatchAttempt(final DomainModel originalModel, final DomainModel replacementModel)
	{
		changePolicy.validateChangeAttempt(originalModel, replacementModel);

		Optional.of(replacementModel)
				.filter(m -> !m.equals(originalModel))
				.ifPresent(insertionPolicy::validateInsertion);
	}

	protected AbstractPatchPolicy(
			final ChangePolicy<DomainModel> changePolicy,
			final InsertionPolicy<DomainModel> insertionPolicy)
	{
		this.changePolicy = changePolicy;
		this.insertionPolicy = insertionPolicy;
	}
}