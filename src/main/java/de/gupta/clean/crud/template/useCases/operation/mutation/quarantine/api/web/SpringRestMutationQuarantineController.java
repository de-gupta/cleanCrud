package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.web;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestMutationQuarantineController
{
	ResponseEntity<Collection<MutationQuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) final int limit);

	ResponseEntity<MutationQuarantineResponse> findById(@PathVariable("id") final String quarantineId);

	ResponseEntity<MutationQuarantineResponse> dismiss(@PathVariable("id") final String quarantineId);

	ResponseEntity<MutationQuarantineResponse> replay(@PathVariable("id") final String quarantineId);
}
