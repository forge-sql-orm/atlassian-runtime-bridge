package com.github.vzakharchenko.runtime.bridge.containers.filters;

import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_ID;
import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_LOGGING_ATTRIBUTES;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Copies Forge ingress logging headers into {@link MDC} for structured logs (Developer Console
 * invocation filtering).
 *
 * <p>Keys match {@link ForgeIngressHeaders} constant values; both are removed in {@code finally}.
 */
@Component
public class InvocationIdLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    try {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String invocationId = httpRequest.getHeader(INVOCATION_ID);
      String invocationLoggingAttributes = httpRequest.getHeader(INVOCATION_LOGGING_ATTRIBUTES);

      if (invocationId != null && !invocationId.isBlank()) {
        MDC.put(INVOCATION_ID, invocationId);
      }

      if (invocationLoggingAttributes != null && !invocationLoggingAttributes.isBlank()) {
        MDC.put(INVOCATION_LOGGING_ATTRIBUTES, invocationLoggingAttributes);
      }

      chain.doFilter(request, response);
    } finally {
      MDC.remove(INVOCATION_ID);
      MDC.remove(INVOCATION_LOGGING_ATTRIBUTES);
    }
  }
}
