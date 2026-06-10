package de.gupta.clean.crud.template.useCases.operation.creation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.creation.application.service.CreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreationApplicationControllerTest
{
	@Test
	void controllerBuildsUserIntentRequestsConveniently()
	{
		var service = new RecordingCreationService();
		var controller = CreationApplicationControllers.controller(service);

		controller.createUserIntent(new OpenOrder());

		assertEquals(OperationSource.USER_INTENT, service.lastRequest.source());
	}

	@Test
	void controllerBuildsAuthoritativeExternalEventRequestsConveniently()
	{
		var service = new RecordingCreationService();
		var controller = CreationApplicationControllers.controller(service);

		controller.createAuthoritativeExternalEvent(new OpenOrder());

		assertEquals(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT, service.lastRequest.source());
		assertEquals(OpenOrder.class, service.lastRequest.payloadType());
	}

	@Test
	void controllerCanPassThroughCorrelationAndCausationMetadata()
	{
		var service = new RecordingCreationService();
		var controller = CreationApplicationControllers.controller(service);

		controller.create(
				new OpenOrder(),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")));

		assertEquals("corr-1", service.lastRequest.correlationId().orElseThrow().value());
		assertEquals("cause-1", service.lastRequest.causationId().orElseThrow().value());
	}

	@Test
	void controllerCanExposeRichCreationResults()
	{
		var service = new RecordingCreationService();
		var controller = CreationApplicationControllers.controller(service);

		var result = controller.createInternalCommandWithResult(new OpenOrder());

		assertEquals("order-1", result.createdOrThrow().domainId());
		assertEquals(OperationSource.INTERNAL_COMMAND, result.context().source());
	}

	private record OpenOrder() implements ApplicationOperationPayload
	{
	}

	private static final class RecordingCreationService implements CreationService<String, String>
	{
		private CreationRequest<?> lastRequest;

		@Override
		public CreationResult<String, String> createWithResult(final CreationRequest<?> request)
		{
			lastRequest = request;
			return CreationResult.created(
					new CreationContext<>(
							Optional.of("order-1"),
							request.source(),
							request.family(),
							request.payloadType(),
							request.correlationId(),
							request.causationId(),
							Optional.empty(),
							Optional.of("ok")),
					CreationPolicyDecision.allow(),
					new CreateResult<>(IdentifiedModel.of("order-1", "ok")));
		}
	}
}