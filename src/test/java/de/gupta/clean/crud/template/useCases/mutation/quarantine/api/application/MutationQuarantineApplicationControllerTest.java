package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.MutationQuarantineService;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MutationQuarantineApplicationControllerTest
{
	@Test
	void controllerDelegatesToServiceWhenAllowed()
	{
		var service = mock(MutationQuarantineService.class);
		var record = record();
		when(service.findOpen(5)).thenReturn(List.of(record));
		when(service.findById(record.quarantineId())).thenReturn(Optional.of(record));
		when(service.dismiss(record.quarantineId())).thenReturn(
				record.dismissed(Instant.parse("2026-06-09T10:16:00Z")));
		var controller = MutationQuarantineApplicationControllers.controller(
				service,
				MutationQuarantineAccessPolicy.allowing());

		Collection<MutationQuarantineRecord> open = controller.findOpen(5);
		var dismissed = controller.dismiss(record.quarantineId());

		assertThat(open).hasSize(1);
		assertThat(dismissed.status()).isEqualTo(MutationQuarantineStatus.DISMISSED);
		verify(service).findOpen(5);
		verify(service).findById(record.quarantineId());
		verify(service).dismiss(record.quarantineId());
	}

	@Test
	void controllerUsesAccessPolicyBeforeOperationalActions()
	{
		var service = mock(MutationQuarantineService.class);
		var record = record();
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

		var exception = assertThrows(
				AccessDeniedException.class,
				() -> controller.replay(record.quarantineId()));

		assertThat(exception.getMessage()).contains("No replay permission");
		verify(service, never()).replay(any());
	}

	private MutationQuarantineRecord record()
	{
		return new MutationQuarantineRecord(
				new MutationQuarantineId("quarantine-1"),
				"aggregate.OrderDefinition",
				String.class.getName(),
				"\"order-1\"",
				"payload.Type",
				"{\"name\":\"value\"}",
				MutationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				MutationFamily.APPLICATION,
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
}