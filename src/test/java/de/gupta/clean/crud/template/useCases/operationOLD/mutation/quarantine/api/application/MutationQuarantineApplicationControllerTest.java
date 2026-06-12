package de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.application.QuarantineApplicationControllers;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.policy.QuarantineAccessPolicy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MutationQuarantineApplicationControllerTest
{
	private QuarantineRecord<MutationReplayInputs> quarantineRecord()
	{
		return new QuarantineRecord<>(
				new QuarantineId("quarantine-1"),
				"aggregate.OrderDefinition",
				new OperationInvocationMetadata(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						OperationFamily.APPLICATION,
						Optional.empty(),
						Optional.empty()),
				new MutationReplayInputs(
						QuarantineReplayEnvelope.of(String.class.getName(), "\"order-1\""),
						QuarantineReplayEnvelope.of("payload.Type", "{\"name\":\"value\"}")),
				List.of(OperationPolicyViolation.externalConsistency("broker mismatch")),
				QuarantineStatus.OPEN,
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	@Nested
	class WhenAllowed
	{
		@Test
		void delegatesToServiceForOpenRecordsAndDismissal()
		{
			@SuppressWarnings("unchecked")
			var service = (QuarantineService<MutationReplayInputs>) mock(QuarantineService.class);
			var record = quarantineRecord();
			when(service.findOpen(5)).thenReturn(List.of(record));
			when(service.findById(record.quarantineId())).thenReturn(Optional.of(record));
			when(service.dismiss(record.quarantineId())).thenReturn(
					record.dismissed(Instant.parse("2026-06-09T10:16:00Z")));
			var controller = QuarantineApplicationControllers.controller(
					service,
					QuarantineAccessPolicy.allowing());

			Collection<QuarantineRecord<MutationReplayInputs>> open = controller.findOpen(5);
			var dismissed = controller.dismiss(record.quarantineId());

			assertThat(open)
					.as("open records should be returned from service")
					.hasSize(1);
			assertThat(dismissed.status())
					.as("dismissed record should have DISMISSED status")
					.isEqualTo(QuarantineStatus.DISMISSED);
			verify(service).findOpen(5);
			verify(service).findById(record.quarantineId());
			verify(service).dismiss(record.quarantineId());
		}
	}

	@Nested
	class WhenAccessPolicyBlocks
	{
		@Test
		void rejectsReplayWhenAccessPolicyDenies()
		{
			@SuppressWarnings("unchecked")
			var service = (QuarantineService<MutationReplayInputs>) mock(QuarantineService.class);
			var record = quarantineRecord();
			when(service.findById(record.quarantineId())).thenReturn(Optional.of(record));
			var controller = QuarantineApplicationControllers.controller(
					service,
					new QuarantineAccessPolicy<>()
					{
						@Override
						public void validateReplay(final QuarantineRecord<MutationReplayInputs> candidate)
						{
							throw AccessDeniedException.withMessage("No replay permission");
						}
					});

			assertThatThrownBy(() -> controller.replay(record.quarantineId()))
					.as("replay should be rejected when access policy denies it")
					.isInstanceOf(AccessDeniedException.class)
					.hasMessageContaining("No replay permission");
			verify(service, never()).replay(any());
		}
	}
}