package de.gupta.clean.crud.template.domain.service.crud.policy;

import java.util.Collection;

public interface ExistingModelsSupplier<DomainModel>
{
	Collection<DomainModel> existingModels();
}