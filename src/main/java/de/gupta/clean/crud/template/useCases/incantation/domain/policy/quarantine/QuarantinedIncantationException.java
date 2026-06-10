package de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine;

public final class QuarantinedIncantationException extends RuntimeException
{
	private final IncantationQuarantineRequest request;

	public static QuarantinedIncantationException withRequest(final IncantationQuarantineRequest request)
	{
		return new QuarantinedIncantationException(request);
	}

	public IncantationQuarantineRequest request()
	{
		return request;
	}

	private QuarantinedIncantationException(final IncantationQuarantineRequest request)
	{
		super("Incantation quarantined");
		this.request = request;
	}
}
