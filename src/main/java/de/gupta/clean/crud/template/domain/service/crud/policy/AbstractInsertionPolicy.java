package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.aletheia.collection.crucible.Crucible;
import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.service.constraints.CollectionConsistenceService;

public abstract class AbstractInsertionPolicy<DomainModel> implements InsertionPolicy<DomainModel>
{
	private final ExistingModelsSupplier<DomainModel> existingModelsSupplier;
	private final CollectionConsistenceService<DomainModel> collectionConsistenceService;

	@Override
	public void validateInsertion(final DomainModel domainModel)
	{
		Unfolding.beckon(Crucible.kindle(existingModelsSupplier.existingModels()).embrace(domainModel))
				 .metamorphose(Crucible::manifest)
				 .unlace(collectionConsistenceService::isThisCollectionConsistent);
	}

	protected AbstractInsertionPolicy(
			final ExistingModelsSupplier<DomainModel> existingModelsSupplier,
			final CollectionConsistenceService<DomainModel> collectionConsistenceService)
	{
		this.existingModelsSupplier = existingModelsSupplier;
		this.collectionConsistenceService = collectionConsistenceService;
	}
}