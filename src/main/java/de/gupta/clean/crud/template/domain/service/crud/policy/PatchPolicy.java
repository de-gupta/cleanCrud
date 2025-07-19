package de.gupta.clean.crud.template.domain.service.crud.policy;

@FunctionalInterface
public interface PatchPolicy<DomainModel>
{
	void validatePatchAttempt(final DomainModel originalModel, final DomainModel replacementModel);
}