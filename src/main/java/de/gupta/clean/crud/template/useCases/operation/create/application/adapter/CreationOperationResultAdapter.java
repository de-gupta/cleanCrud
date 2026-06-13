package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;

@FunctionalInterface
public interface CreationOperationResultAdapter<DomainModel, APIModel>
{
	CreateApplicationResult<APIModel> mapToAPIResult(CreationOperationResult<DomainModel> domainResult);
}