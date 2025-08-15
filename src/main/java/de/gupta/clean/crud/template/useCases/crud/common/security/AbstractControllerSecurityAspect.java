package de.gupta.clean.crud.template.useCases.crud.common.security;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public abstract class AbstractControllerSecurityAspect implements ControllerSecurityAspect
{
	private final ApplicationContext context;

	@Override
	public Object enforcePolicy(ProceedingJoinPoint joinPoint,
								EndpointSecurityConfiguration endpointSecurityConfiguration)
	{
		boolean securityEnabled = Optional.ofNullable(endpointSecurityConfiguration)
										  .map(EndpointSecurityConfiguration::enabled)
										  .orElse(false);

		if (!securityEnabled)
		{
			return tryAndThrow(joinPoint);
		}

		return Optional.of(endpointSecurityConfiguration)
					   .map(EndpointSecurityConfiguration::endpointPolicy)
					   .map(context::getBean)
					   .filter(policy -> policy.isAccessAllowed(
							   ((MethodSignature) joinPoint.getSignature()).getMethod(),
							   joinPoint.getArgs(), getCurrentHttpRequest())
					   )
					   .map(_ -> tryAndThrow(joinPoint))
					   .orElseThrow(() -> AccessDeniedException.withMessage("Access denied by endpoint policy"));
	}

	private Object tryAndThrow(final ProceedingJoinPoint joinPoint)
	{
		try
		{
			return joinPoint.proceed();
		}
		catch (RuntimeException | Error e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}

	private HttpServletRequest getCurrentHttpRequest()
	{
		return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
					   .filter(ServletRequestAttributes.class::isInstance)
					   .map(ServletRequestAttributes.class::cast)
					   .map(ServletRequestAttributes::getRequest)
					   .orElseThrow(() -> new IllegalStateException("No current request"));
	}

	protected AbstractControllerSecurityAspect(final ApplicationContext context)
	{
		this.context = context;
	}
}