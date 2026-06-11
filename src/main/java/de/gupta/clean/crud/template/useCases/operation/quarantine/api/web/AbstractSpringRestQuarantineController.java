package de.gupta.clean.crud.template.useCases.operation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.quarantine.api.application.QuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineId;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public abstract class AbstractSpringRestQuarantineController<P extends PayloadReplayInputs>
		implements SpringRestQuarantineController
{
	private final QuarantineApplicationController<P> applicationController;
	private final QuarantineWebMapper webMapper;

	@Override
	@GetMapping("")
	public ResponseEntity<Collection<QuarantineResponse>> findOpen(
			@RequestParam(value = "limit", defaultValue = "100") @Min(1) final int limit)
	{
		return ResponseEntity.ok(
				applicationController.findOpen(limit).stream().map(webMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{id}")
	public ResponseEntity<QuarantineResponse> findById(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.findById(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/dismiss")
	public ResponseEntity<QuarantineResponse> dismiss(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.dismiss(id(quarantineId))));
	}

	@Override
	@PostMapping("/{id}/replay")
	public ResponseEntity<QuarantineResponse> replay(@PathVariable("id") final String quarantineId)
	{
		return ResponseEntity.ok(webMapper.toResponse(applicationController.replay(id(quarantineId))));
	}

	private QuarantineId id(final String quarantineId)
	{
		return new QuarantineId(quarantineId);
	}

	protected AbstractSpringRestQuarantineController(
			final QuarantineApplicationController<P> applicationController,
			final QuarantineWebMapper webMapper)
	{
		this.applicationController = applicationController;
		this.webMapper = webMapper;
	}
}
