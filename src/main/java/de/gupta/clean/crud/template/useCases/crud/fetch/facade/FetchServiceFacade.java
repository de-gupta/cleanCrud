package de.gupta.clean.crud.template.useCases.crud.fetch.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FetchServiceFacade<APIModelResponse, APIModelID>
{
	Collection<APIModelResponse> findAll();

	Page<APIModelResponse> findAll(final Pageable pageable);

	APIModelResponse findById(final APIModelID id);

	Map<APIModelID, APIModelResponse> findByIds(Set<APIModelID> IDs);
}