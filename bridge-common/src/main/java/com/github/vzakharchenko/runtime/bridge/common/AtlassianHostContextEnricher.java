package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import java.util.Optional;

/**
 * Extension point that lets applications enrich the {@link AtlassianHost} the bridge builds from a
 * Forge invocation token (or any other external context). The Forge security bridge in {@code
 * bridge-forge-connect} starts from a <em>minimal</em> host — {@code installationId}, {@code
 * cloudId}, {@code clientKey}, {@code addonInstalled} — and then walks every registered enricher in
 * ascending {@link #order()} so consumers can attach base URL, shared secret, custom flags,
 * tenant-tier subclasses, etc.
 *
 * <p>Built-in: {@code ConnectOnForgeContext} (in {@code bridge-forge-connect}) looks up the Connect
 * host row by {@code installationId} when the Connect JPA starter is on the classpath, merging
 * persisted columns onto the minimal Forge host without mutating the JPA-managed instance.
 *
 * @param <T> type of the context object used to derive extra host data — typically {@code
 *     com.atlassian.connect.spring.ForgeApiContext} when this bean is registered alongside {@code
 *     bridge-forge-connect}
 */
@FunctionalInterface
public interface AtlassianHostContextEnricher<T> {

  /**
   * Relative position in the enricher chain — lower values run first. Override when your logic must
   * observe (or replace) state produced by another enricher; otherwise the default {@code
   * Integer.MAX_VALUE} runs after every ordered enricher (e.g. after {@code
   * ConnectOnForgeContext.CONNECT_LOOKUP_ORDER = 1}).
   */
  default int order() {
    return Integer.MAX_VALUE;
  }

  /**
   * Applies this enricher's logic and returns the next host in the chain. Implementations are free
   * to:
   *
   * <ul>
   *   <li>return {@code atlassianHost} unchanged (no-op when {@code context} is not relevant),
   *   <li>mutate the supplied host in place when it is not a JPA-managed entity,
   *   <li>build and return a fresh {@code AtlassianHost} (or a subclass carrying extra fields).
   * </ul>
   *
   * <p>Returning {@code Optional.empty()} signals that no host could be resolved for the given
   * context — downstream code then behaves as if no Forge invocation produced a host.
   *
   * @param atlassianHost host produced by the previous step of the chain (may be empty on the very
   *     first call if nothing has built a host yet)
   * @param context the runtime context that triggered the enrichment (e.g. a Forge invocation
   *     token); may be empty for callers that lack one
   * @return the host that subsequent enrichers (and ultimately the security bridge) should see
   */
  Optional<AtlassianHost> update(Optional<AtlassianHost> atlassianHost, Optional<T> context);
}
