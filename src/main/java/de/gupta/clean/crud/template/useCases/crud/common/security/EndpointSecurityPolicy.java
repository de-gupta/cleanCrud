package de.gupta.clean.crud.template.useCases.crud.common.security;


import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

@FunctionalInterface
public interface EndpointSecurityPolicy
{
	boolean isAccessAllowed(Method method, Object[] args, HttpServletRequest request);
}