package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayCodec;

import java.io.IOException;
import java.util.Objects;

public final class JacksonQuarantineReplayCodec implements QuarantineReplayCodec
{
	private final ObjectMapper objectMapper;

	public static JacksonQuarantineReplayCodec with(final ObjectMapper objectMapper)
	{
		return new JacksonQuarantineReplayCodec(objectMapper);
	}

	@Override
	public QuarantineReplayEnvelope serialize(final Object value)
	{
		try
		{
			return QuarantineReplayEnvelope.of(
					value.getClass().getName(),
					objectMapper.writeValueAsString(value));
		}
		catch (JsonProcessingException caught)
		{
			throw new IllegalStateException("Failed to serialize quarantine replay value", caught);
		}
	}

	@Override
	public <T> T deserialize(final QuarantineReplayEnvelope envelope, final Class<T> expectedType)
	{
		try
		{
			return expectedType.cast(objectMapper.readValue(
					envelope.serialized(),
					Class.forName(envelope.typeKey())));
		}
		catch (ClassNotFoundException | IOException caught)
		{
			throw new IllegalStateException("Failed to deserialize quarantine replay value", caught);
		}
	}

	private JacksonQuarantineReplayCodec(final ObjectMapper objectMapper)
	{
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy();
		this.objectMapper.setConfig(
				this.objectMapper.getSerializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
		this.objectMapper.setConfig(
				this.objectMapper.getDeserializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
	}
}