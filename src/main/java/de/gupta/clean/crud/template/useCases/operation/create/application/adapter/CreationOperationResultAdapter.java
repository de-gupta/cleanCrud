package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;

@FunctionalInterface
public interface CreationOperationResultAdapter<DomainModel, APIModel>
{
	CreateAPIResult<APIModel> mapToAPIResult(CreationOperationResult<DomainModel> domainResult);
}