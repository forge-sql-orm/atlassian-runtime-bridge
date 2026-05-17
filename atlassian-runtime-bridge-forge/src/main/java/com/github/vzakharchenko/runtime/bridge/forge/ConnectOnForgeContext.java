package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.ForgeApiContext;
import com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher;
import com.github.vzakharchenko.runtime.bridge.forge.persistence.AtlassianHostByInstallationIdRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Enriches minimally built Forge hosts with Connect-persisted {@link AtlassianHost} data when JPA
 * is available.
 *
 * <p>Runs first ({@link #order()} = {@value #CONNECT_LOOKUP_ORDER}) so later enrichers see stored
 * base URL, secrets metadata, and other columns from the host table.
 */
@Component
@ConditionalOnClass(
    name = "com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration")
public class ConnectOnForgeContext implements AtlassianHostContextEnricher<ForgeApiContext> {

  /** Lowest order value: run before other {@link AtlassianHostContextEnricher} beans. */
  public static final int CONNECT_LOOKUP_ORDER = 1;

  private final Optional<AtlassianHostByInstallationIdRepository> hostByInstallationIdRepository;

  public ConnectOnForgeContext(
      Optional<AtlassianHostByInstallationIdRepository> hostByInstallationIdRepository) {
    this.hostByInstallationIdRepository = hostByInstallationIdRepository;
  }

  @Override
  public int order() {
    return CONNECT_LOOKUP_ORDER;
  }

  @Override
  public Optional<AtlassianHost> update(
      Optional<AtlassianHost> atlassianHost, Optional<ForgeApiContext> context) {
    if (hostByInstallationIdRepository.isEmpty() || atlassianHost.isEmpty()) {
      return atlassianHost;
    }
    AtlassianHost minimal = atlassianHost.get();
    Optional<AtlassianHost> stored =
        hostByInstallationIdRepository.get().findByInstallationId(minimal.getInstallationId());
    return stored.map(host -> AtlassianHostMerge.merge(host, minimal)).or(() -> atlassianHost);
  }
}
