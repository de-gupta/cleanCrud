package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record MutationContext<DomainId, DomainModel>(
		DomainId domainId,
		OperationSource source,
		OperationFamily family,
		Class<? extends ApplicationOperationPayload> payloadType,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId,
		Optional<DomainModel> beforeModel,
		Optional<DomainModel> afterModel)
{
	public MutationContext
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
		Objects.requireNonNull(beforeModel, "beforeModel");
		Objects.requireNonNull(afterModel, "afterModel");
	}
}