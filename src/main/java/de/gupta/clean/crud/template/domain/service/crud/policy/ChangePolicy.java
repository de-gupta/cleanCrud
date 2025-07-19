package de.gupta.clean.crud.template.domain.service.crud.policy;

@FunctionalInterface
public interface ChangePolicy<DomainModel>
{
	void validateChangeAttempt(final DomainModel originalModel, final DomainModel updatedModel);
}