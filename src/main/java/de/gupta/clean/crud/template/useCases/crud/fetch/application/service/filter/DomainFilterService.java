package de.gupta.clean.crud.template.useCases.crud.fetch.application.service.filter;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;

import java.util.Collection;

public interface DomainFilterService<DomainID, DomainModel, DomainModelResponse>
{
	Collection<IdentifiedModel<DomainID, DomainModelResponse>> filter(
			final DomainFilterPipeline<DomainModel> filterPipeline);
}