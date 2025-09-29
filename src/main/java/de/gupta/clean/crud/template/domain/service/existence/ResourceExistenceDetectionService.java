package de.gupta.clean.crud.template.domain.service.existence;

@FunctionalInterface
@Deprecated
public interface ResourceExistenceDetectionService<DomainModel>
{
	boolean existsByModel(final DomainModel model);
}