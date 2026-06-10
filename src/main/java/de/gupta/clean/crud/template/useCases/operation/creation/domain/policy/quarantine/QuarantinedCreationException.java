package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine;

public final class QuarantinedCreationException extends RuntimeException
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
