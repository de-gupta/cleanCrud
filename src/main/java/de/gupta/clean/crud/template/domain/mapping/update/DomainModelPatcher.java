package de.gupta.clean.crud.template.domain.mapping.update;

@FunctionalInterface
public interface DomainModelPatcher<DomainModel, DomainModelUpdatePatch>
{
	DomainModel patchModel(final DomainModel originalModel, final DomainModelUpdatePatch updatePatch);
}