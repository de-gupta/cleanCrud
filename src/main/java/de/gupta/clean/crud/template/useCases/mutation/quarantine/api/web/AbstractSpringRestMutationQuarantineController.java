package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application.MutationQuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class AbstractSpringRestMutationQuarantineController
		implements SpringRestMutationQuarantineController
{
	private final MutationQuarantineApplicationController applicationController;
	private final MutationQuarantineWebMapper webMapper;

	@Override
	@GetMapping("")
	public ResponseEntity<java.util.Collection<MutationQuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) final int limit)
	{
		return ResponseEntity.ok(applicationController.findOpen(limit).stream().map(webMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{id}")
	public ResponseEntity<MutationQuarantineResponse> findById(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.findById(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/dismiss")
	public ResponseEntity<MutationQuarantineResponse> dismiss(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.dismiss(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/replay")
	public ResponseEntity<MutationQuarantineResponse> replay(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.replay(id(quarantineId))));
	}

	private MutationQuarantineId id(final String quarantineId)
	{
		return new MutationQuarantineId(quarantineId);
	}

	protected AbstractSpringRestMutationQuarantineController(
			final MutationQuarantineApplicationController applicationController,
			final MutationQuarantineWebMapper webMapper)
	{
		this.applicationController = applicationController;
		this.webMapper = webMapper;
	}
}