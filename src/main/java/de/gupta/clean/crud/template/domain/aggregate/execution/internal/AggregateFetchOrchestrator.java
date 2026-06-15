package de.gupta.clean.crud.template.domain.aggregate.execution.internal;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Set;

public interface AggregateFetchOrchestrator<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	Collection<IdentifiedModel<DomainId, DomainModel>> findAll(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition);

	Slice<IdentifiedModel<DomainId, DomainModel>> findAll(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final Pageable pageable);

	IdentifiedModel<DomainId, DomainModel> findById(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final DomainId domainId);

	Collection<IdentifiedModel<DomainId, DomainModel>> findByIds(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final Set<DomainId> ids);
}