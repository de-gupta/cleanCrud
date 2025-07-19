package de.gupta.clean.crud.template.useCases.crud.common.security;

import org.aspectj.lang.ProceedingJoinPoint;

public interface ControllerSecurityAspect
{
	Object enforcePolicy(ProceedingJoinPoint joinPoint,
						 EndpointSecurityConfiguration endpointSecurityConfiguration);
}