package de.gupta.clean.crud.template.useCases.operation.quarantine.api.web;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestQuarantineController
{
	ResponseEntity<Collection<QuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) int limit);

	ResponseEntity<QuarantineResponse> findById(@PathVariable("id") String quarantineId);

	ResponseEntity<QuarantineResponse> dismiss(@PathVariable("id") String quarantineId);

	ResponseEntity<QuarantineResponse> replay(@PathVariable("id") String quarantineId);
}
