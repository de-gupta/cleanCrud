package de.gupta.clean.crud.template.useCases.crud.common.adapter.id;

public abstract class AbstractIdentityAPIDomainIDAdapter<ID> implements APIDomainIDAdapter<ID, ID>
{
	@Override
	public ID mapToDomainID(final ID id)
	{
		return id;
	}

	@Override
	public ID mapToAPIModelID(final ID id)
	{
		return id;
	}
}