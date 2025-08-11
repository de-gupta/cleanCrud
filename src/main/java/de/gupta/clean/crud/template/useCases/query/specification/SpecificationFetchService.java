package de.gupta.clean.crud.template.useCases.query.specification;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;

/**
 * Abstraction over a repository capable of executing JPA {@link Specification}s
 * and returning {@link IdentifiedModel}s with Spring Data pagination primitives.
 *
 * Library users can adapt their Spring Data repositories (e.g. JpaSpecificationExecutor)
 * to this interface.
 */
public interface SpecificationFetchService<DomainModel, DomainID>
{
	Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Specification<DomainModel> specification,
															 final Pageable pageable);

	Slice<IdentifiedModel<DomainID, DomainModel>> findAllSlice(final Specification<DomainModel> specification,
																  final Pageable pageable);
}