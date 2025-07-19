package de.gupta.clean.crud.template.domain.mapping.fetch;

@FunctionalInterface
public interface DomainResponseBuilder<DomainModel, DomainModelResponse>
{
	DomainModelResponse toResponse(final DomainModel domainModel);
}