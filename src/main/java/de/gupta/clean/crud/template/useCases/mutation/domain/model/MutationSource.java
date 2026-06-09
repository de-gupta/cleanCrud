package de.gupta.clean.crud.template.useCases.mutation.domain.model;

public enum MutationSource
{
	USER_INTENT,
	INTERNAL_COMMAND,
	AUTHORITATIVE_EXTERNAL_EVENT,
	PROCESS_EMITTED_ACTION,
	ADMINISTRATIVE_REPLAY
}
