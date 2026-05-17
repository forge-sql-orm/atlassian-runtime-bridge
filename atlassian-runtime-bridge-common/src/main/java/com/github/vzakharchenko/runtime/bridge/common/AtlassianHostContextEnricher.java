package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import java.util.Optional;

/**
 * Extension point to enrich {@link AtlassianHost} instances that are created from an external
 * context (for example Forge {@code ForgeApiContext}).
 *
 * <p>The default construction only knows about core identifiers such as {@code cloudId} and {@code
 * installationId}. Implementations of this interface can populate additional host properties (base
 * URL, SEN, client key overrides, custom flags, and so on) so that downstream code can build a
 * richer {@code AtlassianHostUser}.
 *
 * @param <T> type of the context object used to derive extra host data
 */
@FunctionalInterface
public interface AtlassianHostContextEnricher<T> {

  /**
   * Lower values run first when multiple enrichers are registered (see {@code
   * AtlassianForgeSecurityBridgeServiceImpl}).
   */
  default int order() {
    return Integer.MAX_VALUE;
  }

  Optional<AtlassianHost> update(Optional<AtlassianHost> atlassianHost, Optional<T> context);
}
