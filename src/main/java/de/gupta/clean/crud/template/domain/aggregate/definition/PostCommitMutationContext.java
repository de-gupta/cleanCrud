package de.gupta.clean.crud.template.domain.aggregate.definition;

import java.util.Optional;

public record PostCommitMutationContext<DomainId, DomainModel>(
		PostCommitMutationKind kind,
		DomainId domainId,
		Optional<DomainModel> currentModel,
		Optional<DomainModel> previousModel)
{
}