package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.repository.specification;

import de.gupta.clean.crud.template.specification.ModelSpecification;
import org.springframework.data.jpa.domain.Specification;

public interface PersistenceRepositorySpecificationJpaCriteriaAdapter<PersistenceModel, ConcretePersistenceModel>
{
	Specification<ConcretePersistenceModel> toSpecification(final ModelSpecification<PersistenceModel> specification);
}