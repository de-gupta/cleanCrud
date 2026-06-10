package de.gupta.clean.crud.template.useCases.incantation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.incantation.application.service.IncantationService;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.*;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyDecision;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncantationApplicationControllerTest
{
	@Test
	void controllerBuildsUserIntentRequestsConveniently()
	{
		var service = new RecordingIncantationService();
		var controller = IncantationApplicationControllers.controller(service);

		controller.invokeUserIntent(new OpenOrder());

		assertEquals(IncantationSource.USER_INTENT, service.lastRequest.source());
	}

	@Test
	void controllerBuildsAuthoritativeExternalEventRequestsConveniently()
	{
		var service = new RecordingIncantationService();
		var controller = IncantationApplicationControllers.controller(service);

		controller.invokeAuthoritativeExternalEvent(new OpenOrder());

		assertEquals(IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT, service.lastRequest.source());
		assertEquals(OpenOrder.class, service.lastRequest.payloadType());
	}

	@Test
	void controllerCanPassThroughCorrelationAndCausationMetadata()
	{
		var service = new RecordingIncantationService();
		var controller = IncantationApplicationControllers.controller(service);

		controller.invoke(
				new OpenOrder(),
				IncantationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new IncantationCorrelationId("corr-1")),
				Optional.of(new IncantationCausationId("cause-1")));

		assertEquals("corr-1", service.lastRequest.correlationId().orElseThrow().value());
		assertEquals("cause-1", service.lastRequest.causationId().orElseThrow().value());
	}

	@Test
	void controllerCanExposeRichIncantationResults()
	{
		var service = new RecordingIncantationService();
		var controller = IncantationApplicationControllers.controller(service);

		var result = controller.invokeInternalCommandWithResult(new OpenOrder());

		assertEquals("order-1", result.createdOrThrow().domainId());
		assertEquals(IncantationSource.INTERNAL_COMMAND, result.context().source());
	}

	private record OpenOrder() implements ApplicationIncantationPayload
	{
	}

	private static final class RecordingIncantationService implements IncantationService<String, String>
	{
		private IncantationRequest<?> lastRequest;

		@Override
		public IncantationResult<String, String> incantWithResult(final IncantationRequest<?> request)
		{
			lastRequest = request;
			return IncantationResult.created(
					new IncantationContext<>(
							Optional.of("order-1"),
							request.source(),
							request.family(),
							request.payloadType(),
							request.correlationId(),
							request.causationId(),
							Optional.empty(),
							Optional.of("ok")),
					IncantationPolicyDecision.allow(),
					new CreateResult<>(IdentifiedModel.of("order-1", "ok")));
		}
	}
}
