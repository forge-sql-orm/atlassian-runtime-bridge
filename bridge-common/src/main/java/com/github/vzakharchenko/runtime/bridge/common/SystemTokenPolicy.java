package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import org.springframework.util.StringUtils;

/**
 * Runtime-specific check that a system call really carries a Forge <em>system token</em> ({@code
 * appSystemToken}). It is a separate extension point because the runtimes differ: the hybrid
 * runtime receives the token on ingress, while on Forge Containers the token stays in the egress
 * sidecar and cannot be inspected by the app (no policy bean is registered there, so the token
 * check is skipped).
 */
@FunctionalInterface
public interface SystemTokenPolicy {

  /**
   * Returns {@code true} when {@code authentication} carries a <em>valid</em> system token (present
   * and passing every check of the runtime).
   */
  boolean hasSystemToken(ForgeAuthentication authentication);

  /** Returns {@code true} when the request carries a system token at all, valid or not. */
  default boolean isSystemTokenPresent(ForgeAuthentication authentication) {
    var context = authentication.getDetails();
    return context != null && context.getAppToken().filter(StringUtils::hasText).isPresent();
  }
}
