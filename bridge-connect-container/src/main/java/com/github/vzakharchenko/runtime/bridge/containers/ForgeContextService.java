package com.github.vzakharchenko.runtime.bridge.containers;

import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import java.util.Optional;

/**
 * Resolves a Forge {@link ForgeAuthentication} from an ingress {@linkplain
 * ForgeIngressHeaders#INVOCATION_ID invocation id} by calling the egress sidecar {@code
 * /invocation/context} endpoint.
 */
@FunctionalInterface
public interface ForgeContextService {

  /**
   * Builds authentication for the current request, or empty if context cannot be mapped to an
   * {@link com.atlassian.connect.spring.AtlassianHost}.
   *
   * @param invocationId value of {@code x-forge-invocation-id}
   */
  Optional<ForgeAuthentication> emulateForgeAuthentication(String invocationId);
}
