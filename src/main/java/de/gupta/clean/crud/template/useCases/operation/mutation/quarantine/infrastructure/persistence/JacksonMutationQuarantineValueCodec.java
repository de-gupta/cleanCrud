package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.MutationQuarantineValueCodec;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.SerializedMutationValue;

import java.io.IOException;
import java.util.Objects;

public final class JacksonMutationQuarantineValueCodec implements MutationQuarantineValueCodec
{
	private final ObjectMapper objectMapper;

	public static JacksonMutationQuarantineValueCodec with(final ObjectMapper objectMapper)
	{
		return new JacksonMutationQuarantineValueCodec(objectMapper);
	}

	@Override
	public SerializedMutationValue serialize(final Object value)
	{
		try
		{
			return new SerializedMutationValue(
					value.getClass().getName(),
					objectMapper.writeValueAsString(value));
		}
		catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to serialize mutation quarantine value", e);
		}
	}

	@Override
	public <T> T deserialize(final SerializedMutationValue value, final Class<T> expectedType)
	{
		try
		{
			return expectedType.cast(objectMapper.readValue(
					value.valueJson(),
					Class.forName(value.valueType())));
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new IllegalStateException("Failed to deserialize mutation quarantine value", e);
		}
	}

	private JacksonMutationQuarantineValueCodec(final ObjectMapper objectMapper)
	{
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy();
		this.objectMapper.setConfig(
				this.objectMapper.getSerializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
		this.objectMapper.setConfig(
				this.objectMapper.getDeserializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
	}
}
