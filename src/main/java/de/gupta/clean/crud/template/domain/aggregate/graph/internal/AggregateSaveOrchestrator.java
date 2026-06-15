package de.gupta.clean.crud.template.domain.aggregate.graph.internal;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface AggregateSaveOrchestrator<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	Collection<IdentifiedModel<DomainId, DomainModel>> saveAll(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			final Collection<DomainModelCreate> models);
}