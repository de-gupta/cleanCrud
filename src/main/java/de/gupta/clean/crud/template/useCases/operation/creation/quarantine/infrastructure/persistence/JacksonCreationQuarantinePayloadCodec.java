package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantinePayloadCodec;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.SerializedCreationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

import java.io.IOException;
import java.util.Objects;

public final class JacksonCreationQuarantinePayloadCodec implements CreationQuarantinePayloadCodec
{
	private final ObjectMapper objectMapper;

	public static JacksonCreationQuarantinePayloadCodec with(final ObjectMapper objectMapper)
	{
		return new JacksonCreationQuarantinePayloadCodec(objectMapper);
	}

	@Override
	public SerializedCreationPayload serialize(final ApplicationOperationPayload payload)
	{
		try
		{
			return new SerializedCreationPayload(
					payload.getClass().getName(),
					objectMapper.writeValueAsString(payload));
		}
		catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to serialize creation quarantine payload", e);
		}
	}

	@Override
	public ApplicationOperationPayload deserialize(final SerializedCreationPayload payload)
	{
		try
		{
			return (ApplicationOperationPayload) objectMapper.readValue(
					payload.payloadJson(),
					Class.forName(payload.payloadType()));
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new IllegalStateException("Failed to deserialize creation quarantine payload", e);
		}
	}

	private JacksonCreationQuarantinePayloadCodec(final ObjectMapper objectMapper)
	{
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy();
		this.objectMapper.setConfig(
				this.objectMapper.getSerializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
		this.objectMapper.setConfig(
				this.objectMapper.getDeserializationConfig().without(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));
	}
}
