package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * Decides whether the current request is a Forge <em>system call</em>, i.e. a {@link
 * ForgeAuthentication} that has no user. Both the invocation token principal and the {@link
 * AtlassianHostUser} account id are inspected, because depending on the runtime and filter stage
 * only one of them may be populated.
 *
 * <p>Fails closed: anything that is not a {@link ForgeAuthentication} is not a system call.
 */
public final class SystemCallVerifier {

  private SystemCallVerifier() {}

  /** Returns {@code true} when {@code authentication} is a Forge call without a user. */
  public static boolean isSystemCall(Authentication authentication) {
    if (!(authentication instanceof ForgeAuthentication forgeAuthentication)) {
      return false;
    }
    if (forgeAuthentication.getPrincipal() instanceof AtlassianHostUser hostUser
        && hostUser.getUserAccountId().filter(StringUtils::hasText).isPresent()) {
      return false;
    }
    var apiContext = forgeAuthentication.getDetails();
    if (apiContext != null && apiContext.getForgeInvocationToken() != null) {
      return !StringUtils.hasText(apiContext.getForgeInvocationToken().getPrincipal());
    }
    return true;
  }

  /**
   * Checks the {@link SecurityContextHolder} authentication.
   *
   * @throws AccessDeniedException if the current call is not a user-less Forge call
   */
  public static void assertSystemCall() {
    assertSystemCall(null);
  }

  /**
   * Like {@link #assertSystemCall()}, and additionally requires a system token when a {@link
   * SystemTokenPolicy} is supplied ({@code null} skips the token check).
   *
   * @throws AccessDeniedException if the call has a user or lacks a system token
   */
  public static void assertSystemCall(SystemTokenPolicy tokenPolicy) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!isSystemCall(authentication)) {
      throw new AccessDeniedException(
          "System call expected: Forge invocation without userId is required");
    }
    if (tokenPolicy != null && !tokenPolicy.hasSystemToken((ForgeAuthentication) authentication)) {
      throw new AccessDeniedException("System call expected: Forge system token is required");
    }
  }
}
