package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MutationApplicationControllerTest
{
	@Test
	void controllerBuildsUserIntentRequestsConveniently()
	{
		var service = new RecordingMutationService();
		var controller = MutationApplicationControllers.controller(service);

		controller.applyUserIntent("order-1", new AcknowledgeOrder());

		assertEquals("order-1", service.lastRequest.domainId());
		assertEquals(MutationSource.USER_INTENT, service.lastRequest.source());
	}

	@Test
	void controllerBuildsAuthoritativeExternalEventRequestsConveniently()
	{
		var service = new RecordingMutationService();
		var controller = MutationApplicationControllers.controller(service);

		controller.applyAuthoritativeExternalEvent("order-1", new AcknowledgeOrder());

		assertEquals(MutationSource.AUTHORITATIVE_EXTERNAL_EVENT, service.lastRequest.source());
		assertEquals(AcknowledgeOrder.class, service.lastRequest.payloadType());
	}

	@Test
	void controllerCanPassThroughCorrelationAndCausationMetadata()
	{
		var service = new RecordingMutationService();
		var controller = MutationApplicationControllers.controller(service);

		controller.apply(
				"order-1",
				new AcknowledgeOrder(),
				MutationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new MutationCorrelationId("corr-1")),
				Optional.of(new MutationCausationId("cause-1")));

		assertEquals("corr-1", service.lastRequest.correlationId().orElseThrow().value());
		assertEquals("cause-1", service.lastRequest.causationId().orElseThrow().value());
	}

	private record AcknowledgeOrder() implements ApplicationMutationPayload
	{
	}

	private static final class RecordingMutationService implements MutationService<String, String>
	{
		private MutationRequest<String, ?> lastRequest;

		@Override
		public IdentifiedModel<String, String> mutate(final MutationRequest<String, ?> request)
		{
			lastRequest = request;
			return IdentifiedModel.of(request.domainId(), "ok");
		}
	}
}
