package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.quarantine;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

public final class QuarantinedMutationException extends DomainException
{
	private final MutationQuarantineRequest request;

	public static QuarantinedMutationException withRequest(final MutationQuarantineRequest request)
	{
		return new QuarantinedMutationException(request);
	}

	public MutationQuarantineRequest request()
	{
		return request;
	}

	private QuarantinedMutationException(final MutationQuarantineRequest request)
	{
		super("Mutation was quarantined: " + request.violations());
		this.request = request;
	}
}