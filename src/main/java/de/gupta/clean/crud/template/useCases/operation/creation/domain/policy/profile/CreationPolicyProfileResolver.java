package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

@FunctionalInterface
public interface CreationPolicyProfileResolver
{
	static CreationPolicyProfileResolver defaultResolver()
	{
		return source -> switch (source)
		{
			case USER_INTENT -> CreationPolicyProfile.userIntent();
			case INTERNAL_COMMAND, PROCESS_EMITTED_ACTION, ADMINISTRATIVE_REPLAY ->
					CreationPolicyProfile.internalCommand();
			case AUTHORITATIVE_EXTERNAL_EVENT -> CreationPolicyProfile.authoritativeExternalEvent();
		};
	}

	CreationPolicyProfile resolve(final OperationSource source);
}
