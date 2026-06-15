package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.application.adapter.AbstractCreateOperationResultAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreatedCreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.AbstractCreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreateExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluator;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CreateApplicationControllerFlowTest
{
	@Test
	void create_routesThroughControllerFacadeAndService_withHandlerSelectionAndStableResultShape()
	{
		var selectedPayloadName = new AtomicReference<String>();
		var executedAttempt = new AtomicReference<PreparedCreationAttempt<DomainPayload, String>>();
		var registry = CreationHandlerRegistry.of(List.of(
				RegisteredCreationHandler.of(DomainPayload.class, request ->
				{
					selectedPayloadName.set(request.payload().name());
					return CreationPlan.of("aggregate.Task", "domain:" + request.payload().name());
				})));
		var evaluator = CreationPolicyEvaluator.of(List.of(_ -> CreationPolicyEvaluation.allow(List.of(
				CreationOperationViolation.externalConsistency("accepted with warning")))));
		CreateExecutor<DomainPayload, String, String> executor = attempt ->
		{
			executedAttempt.set(attempt);
			return "created:" + attempt.plan().createModel();
		};

		var service = new TestCreateApplicationService(registry, evaluator, executor);
		var facade = new TestCreateApplicationServiceFacade(service);
		var controller = new TestCreateApplicationController(facade);

		var result = controller.create(new CreateOperationRequest<>(
				new ApiPayload("sample"),
				OperationRequestMetadata.source(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)));

		assertThat(selectedPayloadName).hasValue("sample");
		assertThat(executedAttempt).hasValueSatisfying(attempt ->
		{
			assertThat(attempt.request().payload()).isEqualTo(new DomainPayload("sample"));
			assertThat(attempt.context().payloadTypeName()).isEqualTo(DomainPayload.class.getName());
			assertThat(attempt.plan()).isEqualTo(CreationPlan.of("aggregate.Task", "domain:sample"));
		});
		assertThat(result).isInstanceOf(CreatedCreateApplicationResult.class);
		var created = (CreatedCreateApplicationResult<String>) result;
		assertThat(created.context().source()).isEqualTo(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
		assertThat(created.context().payloadTypeName()).isEqualTo(DomainPayload.class.getName());
		assertThat(created.createdModel()).isEqualTo("api:created:domain:sample");
		assertThat(created.toleratedViolations()).singleElement().satisfies(violation ->
		{
			assertThat(violation.kind().name()).isEqualTo("EXTERNAL_CONSISTENCY");
			assertThat(violation.message()).isEqualTo("accepted with warning");
		});
	}

	private record ApiPayload(String name) implements CreateOperationPayload
	{
	}

	private record DomainPayload(String name) implements CreateOperationPayload
	{
	}

	private static final class TestCreateApplicationService
			extends AbstractCreateApplicationService<DomainPayload, String, String>
	{
		private TestCreateApplicationService(
				final CreationHandlerRegistry<String> handlerRegistry,
				final CreationPolicyEvaluator policyEvaluator,
				final CreateExecutor<DomainPayload, String, String> createExecutor)
		{
			super(handlerRegistry, policyEvaluator, createExecutor);
		}
	}

	private static final class TestCreateApplicationServiceFacade
			extends AbstractCreateApplicationServiceFacade<ApiPayload, DomainPayload, String, String>
	{
		private TestCreateApplicationServiceFacade(
				final TestCreateApplicationService service)
		{
			super(service, apiPayload -> new DomainPayload(apiPayload.name()), new TestResultAdapter());
		}
	}

	private static final class TestCreateApplicationController
			extends AbstractCreateApplicationController<ApiPayload, String>
	{
		private TestCreateApplicationController(final CreateApplicationServiceFacade<ApiPayload, String> serviceFacade)
		{
			super(serviceFacade);
		}
	}

	private static final class TestResultAdapter extends AbstractCreateOperationResultAdapter<String, String>
	{
		@Override
		protected String mapCreatedModel(final String domainModel)
		{
			return "api:" + domainModel;
		}
	}
}