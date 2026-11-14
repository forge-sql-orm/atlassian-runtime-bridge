package com.github.vzakharchenko.runtime.bridge.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Enforces {@link SystemCallOnly} on handler methods and their declaring controllers. */
public class SystemCallInterceptor implements HandlerInterceptor {

  private final SystemTokenPolicy tokenPolicy;

  /**
   * @param tokenPolicy optional system-token check; {@code null} verifies only the absence of a
   *     user
   */
  public SystemCallInterceptor(SystemTokenPolicy tokenPolicy) {
    this.tokenPolicy = tokenPolicy;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (handler instanceof HandlerMethod method && isSystemCallOnly(method)) {
      SystemCallVerifier.assertSystemCall(tokenPolicy);
    }
    return true;
  }

  private static boolean isSystemCallOnly(HandlerMethod method) {
    return AnnotatedElementUtils.hasAnnotation(method.getMethod(), SystemCallOnly.class)
        || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), SystemCallOnly.class);
  }
}
