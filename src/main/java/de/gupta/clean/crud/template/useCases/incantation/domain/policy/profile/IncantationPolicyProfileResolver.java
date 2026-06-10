package de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;

@FunctionalInterface
public interface IncantationPolicyProfileResolver
{
	static IncantationPolicyProfileResolver defaultResolver()
	{
		return source -> switch (source)
		{
			case USER_INTENT -> IncantationPolicyProfile.userIntent();
			case INTERNAL_COMMAND, PROCESS_EMITTED_ACTION, ADMINISTRATIVE_REPLAY ->
					IncantationPolicyProfile.internalCommand();
			case AUTHORITATIVE_EXTERNAL_EVENT -> IncantationPolicyProfile.authoritativeExternalEvent();
		};
	}

	IncantationPolicyProfile resolve(final IncantationSource source);
}
