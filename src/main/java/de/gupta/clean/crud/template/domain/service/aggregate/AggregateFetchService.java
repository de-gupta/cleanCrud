package de.gupta.clean.crud.template.domain.service.aggregate;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Set;

public interface AggregateFetchService<DomainId, DomainModel>
{
	Collection<IdentifiedModel<DomainId, DomainModel>> findAll();

	Slice<IdentifiedModel<DomainId, DomainModel>> findAll(Pageable pageable);

	IdentifiedModel<DomainId, DomainModel> findById(DomainId domainId);

	Collection<IdentifiedModel<DomainId, DomainModel>> findByIds(Set<DomainId> ids);
}