package de.gupta.clean.crud.template.domain.service.aggregate.operation;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface AggregateSaveService<DomainId, DomainModel, DomainModelCreate>
{
	default IdentifiedModel<DomainId, DomainModel> save(final DomainModelCreate model)
	{
		return saveAll(List.of(model)).stream().findFirst().orElseThrow();
	}

	default Collection<IdentifiedModel<DomainId, DomainModel>> saveAll(final Collection<DomainModelCreate> models)
	{
		return saveAll(models, _ -> List.of());
	}

	Collection<IdentifiedModel<DomainId, DomainModel>> saveAll(final Collection<DomainModelCreate> models,
	                                                           final Function<Collection<IdentifiedModel<DomainId, DomainModel>>,
																	   Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests);
}