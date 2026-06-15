package de.gupta.clean.crud.template.domain.aggregate.port;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface AggregateFetchPort<DomainId, DomainModel>
{
	Optional<IdentifiedModel<DomainId, DomainModel>> findById(DomainId domainId);

	Collection<IdentifiedModel<DomainId, DomainModel>> findByIds(Set<DomainId> domainIds);

	Collection<IdentifiedModel<DomainId, DomainModel>> findAll();

	Slice<IdentifiedModel<DomainId, DomainModel>> findAll(Pageable pageable);
}