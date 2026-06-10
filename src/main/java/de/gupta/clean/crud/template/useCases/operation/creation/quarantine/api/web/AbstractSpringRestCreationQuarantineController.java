package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application.CreationQuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class AbstractSpringRestCreationQuarantineController
		implements SpringRestCreationQuarantineController
{
	private final CreationQuarantineApplicationController applicationController;
	private final CreationQuarantineWebMapper webMapper;

	@Override
	@GetMapping("")
	public ResponseEntity<java.util.Collection<CreationQuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) final int limit)
	{
		return ResponseEntity.ok(applicationController.findOpen(limit).stream().map(webMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{id}")
	public ResponseEntity<CreationQuarantineResponse> findById(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.findById(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/dismiss")
	public ResponseEntity<CreationQuarantineResponse> dismiss(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.dismiss(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/replay")
	public ResponseEntity<CreationQuarantineResponse> replay(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.replay(id(quarantineId))));
	}

	private CreationQuarantineId id(final String quarantineId)
	{
		return new CreationQuarantineId(quarantineId);
	}

	protected AbstractSpringRestCreationQuarantineController(
			final CreationQuarantineApplicationController applicationController,
			final CreationQuarantineWebMapper webMapper)
	{
		this.applicationController = applicationController;
		this.webMapper = webMapper;
	}
}
