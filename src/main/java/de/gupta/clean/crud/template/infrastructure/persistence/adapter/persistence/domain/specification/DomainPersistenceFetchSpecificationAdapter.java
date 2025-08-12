package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.specification;

import de.gupta.clean.crud.template.specification.ModelSpecification;

public interface DomainPersistenceFetchSpecificationAdapter<DomainModel, PersistenceModel>
{
	ModelSpecification<PersistenceModel> toPersistenceSpecification(
			final ModelSpecification<DomainModel> specification);
}