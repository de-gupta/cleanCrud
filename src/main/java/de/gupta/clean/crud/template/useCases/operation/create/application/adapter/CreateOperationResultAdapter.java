package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationResult;

@FunctionalInterface
public interface CreateOperationResultAdapter<DomainModel, APIModel>
{
	CreateApplicationResult<APIModel> mapToAPIResult(CreateOperationResult<DomainModel> domainResult);
}