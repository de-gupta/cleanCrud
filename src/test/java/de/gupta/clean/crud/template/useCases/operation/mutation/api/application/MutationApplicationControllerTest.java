package de.gupta.clean.crud.template.useCases.operation.mutation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.MutationPolicyDecision;
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
		assertEquals(OperationSource.USER_INTENT, service.lastRequest.source());
	}

	@Test
	void controllerBuildsAuthoritativeExternalEventRequestsConveniently()
	{
		var service = new RecordingMutationService();
		var controller = MutationApplicationControllers.controller(service);

		controller.applyAuthoritativeExternalEvent("order-1", new AcknowledgeOrder());

		assertEquals(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT, service.lastRequest.source());
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
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1")));

		assertEquals("corr-1", service.lastRequest.correlationId().orElseThrow().value());
		assertEquals("cause-1", service.lastRequest.causationId().orElseThrow().value());
	}

	@Test
	void controllerCanExposeRichMutationResults()
	{
		var service = new RecordingMutationService();
		var controller = MutationApplicationControllers.controller(service);

		var result = controller.applyInternalCommandWithResult("order-1", new AcknowledgeOrder());

		assertEquals("order-1", result.updatedOrThrow().id());
		assertEquals(OperationSource.INTERNAL_COMMAND, result.context().source());
	}

	private record AcknowledgeOrder() implements ApplicationOperationPayload
	{
	}

	private static final class RecordingMutationService implements MutationService<String, String>
	{
		private MutationRequest<String, ?> lastRequest;

		@Override
		public MutationResult<String, String> mutateWithResult(final MutationRequest<String, ?> request)
		{
			lastRequest = request;
			return MutationResult.applied(
					new de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationContext<>(
							request.domainId(),
							request.source(),
							request.family(),
							request.payloadType(),
							request.correlationId(),
							request.causationId(),
							Optional.empty(),
							Optional.of("ok")),
					MutationPolicyDecision.allow(),
					IdentifiedModel.of(request.domainId(), "ok"));
		}
	}
}