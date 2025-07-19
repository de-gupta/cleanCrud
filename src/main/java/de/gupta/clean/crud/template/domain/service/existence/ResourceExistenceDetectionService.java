package de.gupta.clean.crud.template.domain.service.existence;

@FunctionalInterface
public interface ResourceExistenceDetectionService<DomainModel>
{
	boolean existsByModel(final DomainModel model);
}