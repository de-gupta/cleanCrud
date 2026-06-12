package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

@FunctionalInterface
public interface MutationPolicyProfileResolver
{
	static MutationPolicyProfileResolver defaultResolver()
	{
		return source -> switch (source)
		{
			case USER_INTENT -> MutationPolicyProfile.userIntent();
			case INTERNAL_COMMAND, PROCESS_EMITTED_ACTION, ADMINISTRATIVE_REPLAY ->
					MutationPolicyProfile.internalCommand();
			case AUTHORITATIVE_EXTERNAL_EVENT -> MutationPolicyProfile.authoritativeExternalEvent();
		};
	}

	MutationPolicyProfile resolve(final OperationSource source);
}