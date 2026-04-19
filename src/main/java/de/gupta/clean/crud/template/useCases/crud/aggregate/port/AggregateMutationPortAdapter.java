package de.gupta.clean.crud.template.useCases.crud.aggregate.port;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeletePersistenceService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SavePersistenceService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdatePersistenceService;

public final class AggregateMutationPortAdapter<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
		implements AggregateMutationPort<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
{
	private final SavePersistenceService<DomainId, DomainModel> savePersistenceService;
	private final UpdatePersistenceService<DomainId, DomainModel> updatePersistenceService;
	private final DeletePersistenceService<DomainId> deletePersistenceService;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
	AggregateMutationPortAdapter<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
	withPersistenceServices(
			final SavePersistenceService<DomainId, DomainModel> savePersistenceService,
			final UpdatePersistenceService<DomainId, DomainModel> updatePersistenceService,
			final DeletePersistenceService<DomainId> deletePersistenceService)
	{
		return new AggregateMutationPortAdapter<>(savePersistenceService, updatePersistenceService,
				deletePersistenceService);
	}

	private AggregateMutationPortAdapter(
			final SavePersistenceService<DomainId, DomainModel> savePersistenceService,
			final UpdatePersistenceService<DomainId, DomainModel> updatePersistenceService,
			final DeletePersistenceService<DomainId> deletePersistenceService)
	{
		this.savePersistenceService = savePersistenceService;
		this.updatePersistenceService = updatePersistenceService;
		this.deletePersistenceService = deletePersistenceService;
	}

	@Override
	public IdentifiedModel<DomainId, DomainModel> create(final DomainModel domainModel)
	{
		return savePersistenceService.save(domainModel);
	}

	@Override
	public void put(final DomainId domainId, final DomainModel domainModel)
	{
		updatePersistenceService.putAtId(domainId, domainModel);
	}

	@Override
	public IdentifiedModel<DomainId, DomainModel> update(final DomainId domainId, final DomainModel domainModel)
	{
		return updatePersistenceService.updateById(domainId, domainModel);
	}

	@Override
	public void delete(final DomainId domainId)
	{
		deletePersistenceService.deleteById(domainId);
	}
}
