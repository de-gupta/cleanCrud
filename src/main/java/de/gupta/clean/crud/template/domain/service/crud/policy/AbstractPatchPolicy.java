package de.gupta.clean.crud.template.domain.service.crud.policy;

import de.gupta.aletheia.collection.crucible.Crucible;
import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.service.constraints.CollectionConsistenceService;

public abstract class AbstractPatchPolicy<DomainModel> implements PatchPolicy<DomainModel>
{
	private final ChangePolicy<DomainModel> changePolicy;
	private final ExistingModelsSupplier<DomainModel> existingModelsSupplier;
	private final CollectionConsistenceService<DomainModel> collectionConsistenceService;

	@Override
	public void validatePatchAttempt(final DomainModel originalModel, final DomainModel replacementModel)
	{
		changePolicy.validateChangeAttempt(originalModel, replacementModel);

		Unfolding.beckon(Crucible.kindle(existingModelsSupplier.existingModels())
								 .banish(originalModel)
								 .embrace(replacementModel))
				 .metamorphose(Crucible::manifest)
				 .unlace(collectionConsistenceService::isThisCollectionConsistent);
	}

	protected AbstractPatchPolicy(
			final ChangePolicy<DomainModel> changePolicy,
			final ExistingModelsSupplier<DomainModel> existingModelsSupplier,
			final CollectionConsistenceService<DomainModel> collectionConsistenceService)
	{
		this.changePolicy = changePolicy;
		this.existingModelsSupplier = existingModelsSupplier;
		this.collectionConsistenceService = collectionConsistenceService;
	}
}