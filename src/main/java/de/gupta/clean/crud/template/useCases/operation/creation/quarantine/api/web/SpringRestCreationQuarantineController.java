package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestCreationQuarantineController
{
	ResponseEntity<Collection<CreationQuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) final int limit);

	ResponseEntity<CreationQuarantineResponse> findById(@PathVariable("id") final String quarantineId);

	ResponseEntity<CreationQuarantineResponse> dismiss(@PathVariable("id") final String quarantineId);

	ResponseEntity<CreationQuarantineResponse> replay(@PathVariable("id") final String quarantineId);
}
