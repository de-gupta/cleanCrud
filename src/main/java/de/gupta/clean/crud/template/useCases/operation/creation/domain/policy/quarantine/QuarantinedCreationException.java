package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class QuarantinedCreationException extends DomainException
{
	private final CreationQuarantineRequest request;

	public static QuarantinedCreationException withRequest(final CreationQuarantineRequest request)
	{
		return new QuarantinedCreationException(request);
	}

	public CreationQuarantineRequest request()
	{
		return request;
	}

	private QuarantinedCreationException(final CreationQuarantineRequest request)
	{
		super("Creation quarantined");
		this.request = request;
	}
}
