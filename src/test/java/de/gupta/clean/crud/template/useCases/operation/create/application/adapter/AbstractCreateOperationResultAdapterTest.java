package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.*;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationResults;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCreateOperationResultAdapterTest
{
	private final TestResultAdapter adapter = new TestResultAdapter();

	@Test
	void mapToApiResult_mapsCreatedDomainResult()
	{
		var tolerated = CreateOperationViolation.externalConsistency("accepted with warning");
		var result = adapter.mapToAPIResult(CreateOperationResults.created(context(), "domain-created", List.of(
				tolerated)));

		assertThat(result).isInstanceOf(CreatedCreateApplicationResult.class);
		var created = (CreatedCreateApplicationResult<String>) result;
		assertThat(created.context().source()).isEqualTo(OperationSource.USER_INTENT);
		assertThat(created.context().payloadTypeName()).isEqualTo("payload.Type");
		assertThat(created.context().correlationId()).contains("corr-1");
		assertThat(created.context().causationId()).contains("cause-1");
		assertThat(created.createdModel()).isEqualTo("api:domain-created");
		assertThat(created.toleratedViolations()).containsExactly(new CreateApplicationViolation(
				CreateApplicationViolationKind.EXTERNAL_CONSISTENCY,
				"accepted with warning"));
	}

	private static CreateOperationContext context()
	{
		return new CreateOperationContext(
				OperationSource.USER_INTENT,
				"payload.Type",
				Optional.of("corr-1"),
				Optional.of("cause-1"));
	}

	@Test
	void mapToApiResult_mapsRejectedDomainResult_withViolationsAndContextPreserved()
	{
		var blocking = CreateOperationViolation.core("core rule failed");
		var tolerated = CreateOperationViolation.invariant("soft warning");
		var result = adapter.mapToAPIResult(CreateOperationResults.rejected(
				context(),
				List.of(blocking),
				List.of(tolerated)));

		assertThat(result).isInstanceOf(RejectedCreateApplicationResult.class);
		var rejected = (RejectedCreateApplicationResult<String>) result;
		assertThat(rejected.context().payloadTypeName()).isEqualTo("payload.Type");
		assertThat(rejected.blockingViolations()).containsExactly(new CreateApplicationViolation(
				CreateApplicationViolationKind.CORE,
				"core rule failed"));
		assertThat(rejected.toleratedViolations()).containsExactly(new CreateApplicationViolation(
				CreateApplicationViolationKind.INVARIANT,
				"soft warning"));
	}

	@Test
	void mapToApiResult_mapsQuarantinedDomainResult_withReferenceAndContextPreserved()
	{
		var blocking = CreateOperationViolation.access("manual review needed");
		var result = adapter.mapToAPIResult(CreateOperationResults.quarantined(
				context(),
				List.of(blocking),
				List.of(),
				Optional.of("Q-17")));

		assertThat(result).isInstanceOf(QuarantinedCreateApplicationResult.class);
		var quarantined = (QuarantinedCreateApplicationResult<String>) result;
		assertThat(quarantined.context().payloadTypeName()).isEqualTo("payload.Type");
		assertThat(quarantined.blockingViolations()).containsExactly(new CreateApplicationViolation(
				CreateApplicationViolationKind.ACCESS,
				"manual review needed"));
		assertThat(quarantined.quarantineReference()).contains("Q-17");
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