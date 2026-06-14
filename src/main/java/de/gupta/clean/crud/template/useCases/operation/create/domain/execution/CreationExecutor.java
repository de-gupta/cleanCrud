package de.gupta.clean.crud.template.useCases.operation.create.domain.execution;

import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

@FunctionalInterface
public interface CreationExecutor<DomainCreateModel, DomainCreatedModel>
{
	DomainCreatedModel create(CreationPlan<DomainCreateModel> plan);
}