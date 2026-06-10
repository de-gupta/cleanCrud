package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

public interface MutationQuarantineValueCodec
{
	SerializedMutationValue serialize(Object value);

	<T> T deserialize(SerializedMutationValue value, Class<T> expectedType);
}
