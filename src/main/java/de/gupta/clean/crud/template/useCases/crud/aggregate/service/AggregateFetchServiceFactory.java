package de.gupta.clean.crud.template.useCases.crud.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.runtime.AggregateWorkflowRunner;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateFetchService;
import de.gupta.clean.crud.template.domain.service.aggregate.DefaultAggregateFetchService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;

public enum AggregateFetchServiceFactory
{
	;

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateWorkflowRunner engine)
	{
		return fetchService(DefaultAggregateFetchService.create(definition, engine));
	}

	private static <MasterDomainId, MasterDomainModel> FetchService<MasterDomainModel, MasterDomainId> fetchService(
			final AggregateFetchService<MasterDomainId, MasterDomainModel> aggregateFetchService)
	{
		return new FetchAggregateCrudService<>(aggregateFetchService);
	}

	private static final class FetchAggregateCrudService<MasterDomainId, MasterDomainModel>
			extends AbstractFetchService<MasterDomainId, MasterDomainModel>
			implements FetchService<MasterDomainModel, MasterDomainId>
	{
		private FetchAggregateCrudService(
				final AggregateFetchService<MasterDomainId, MasterDomainModel> aggregateFetchService)
		{
			super(aggregateFetchService);
		}
	}
}