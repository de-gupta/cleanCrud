package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.MutationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;
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
	private MutationQuarantineRecord quarantineRecord()
	{
		return new MutationQuarantineRecord(
				new MutationQuarantineId("quarantine-1"),
				"aggregate.OrderDefinition",
				QuarantineReplayEnvelope.of(String.class.getName(), "\"order-1\""),
				QuarantineReplayEnvelope.of("payload.Type", "{\"name\":\"value\"}"),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				OperationFamily.APPLICATION,
				Optional.empty(),
				Optional.empty(),
				MutationQuarantineStatus.OPEN,
				List.of(MutationPolicyViolation.externalConsistency("broker mismatch")),
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
			var service = mock(MutationQuarantineService.class);
			var record = quarantineRecord();
			when(service.findOpen(5)).thenReturn(List.of(record));
			when(service.findById(record.quarantineId())).thenReturn(Optional.of(record));
			when(service.dismiss(record.quarantineId())).thenReturn(
					record.dismissed(Instant.parse("2026-06-09T10:16:00Z")));
			var controller = MutationQuarantineApplicationControllers.controller(
					service,
					MutationQuarantineAccessPolicy.allowing());

			Collection<MutationQuarantineRecord> open = controller.findOpen(5);
			var dismissed = controller.dismiss(record.quarantineId());

			assertThat(open)
					.as("open records should be returned from service")
					.hasSize(1);
			assertThat(dismissed.status())
					.as("dismissed record should have DISMISSED status")
					.isEqualTo(MutationQuarantineStatus.DISMISSED);
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
			var service = mock(MutationQuarantineService.class);
			var record = quarantineRecord();
			when(service.findById(record.quarantineId())).thenReturn(Optional.of(record));
			var controller = MutationQuarantineApplicationControllers.controller(
					service,
					new MutationQuarantineAccessPolicy()
					{
						@Override
						public void validateReplay(final MutationQuarantineRecord candidate)
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