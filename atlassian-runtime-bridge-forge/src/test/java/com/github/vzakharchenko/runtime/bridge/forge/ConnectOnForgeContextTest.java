package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.github.vzakharchenko.runtime.bridge.forge.persistence.AtlassianHostByInstallationIdRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectOnForgeContextTest {

  private static final String INSTALLATION_ID = "ari:cloud:ecosystem::installation/abc";
  private static final String CLOUD_FROM_FIT = "cloud-from-fit";

  @Mock private AtlassianHostByInstallationIdRepository hostByInstallationIdRepository;

  private ConnectOnForgeContext enricher;

  @BeforeEach
  void setUp() {
    enricher = new ConnectOnForgeContext(Optional.of(hostByInstallationIdRepository));
  }

  @Test
  void order_isLowestSoConnectLookupRunsFirst() {
    assertThat(enricher.order()).isEqualTo(ConnectOnForgeContext.CONNECT_LOOKUP_ORDER);
  }

  @Test
  void update_returnsMinimalHostWhenRepositoryBeanMissing() {
    ConnectOnForgeContext withoutRepo = new ConnectOnForgeContext(Optional.empty());
    AtlassianHost minimal = minimalHost(CLOUD_FROM_FIT);

    assertThat(withoutRepo.update(Optional.of(minimal), Optional.empty())).contains(minimal);
  }

  @Test
  void update_mergesBaseUrlFromStoredHostRow() {
    AtlassianHost minimal = minimalHost(CLOUD_FROM_FIT);
    AtlassianHost stored = storedHost();

    when(hostByInstallationIdRepository.findByInstallationId(INSTALLATION_ID))
        .thenReturn(Optional.of(stored));

    AtlassianHost merged = enricher.update(Optional.of(minimal), Optional.empty()).orElseThrow();

    assertThat(merged.getBaseUrl())
        .as("stored host row supplies Connect-persisted fields")
        .isEqualTo("https://tenant.atlassian.net");
  }

  @Test
  void update_doesNotMutateRowReturnedFromRepository() {
    AtlassianHost minimal = minimalHost(CLOUD_FROM_FIT);
    AtlassianHost stored = storedHost();

    when(hostByInstallationIdRepository.findByInstallationId(INSTALLATION_ID))
        .thenReturn(Optional.of(stored));

    enricher.update(Optional.of(minimal), Optional.empty());

    assertThat(stored.getCloudId()).isEqualTo("stale-cloud");
  }

  @Test
  void update_preservesCloudIdFromForgeInvocationOverStoredRow() {
    AtlassianHost minimal = minimalHost(CLOUD_FROM_FIT);
    when(hostByInstallationIdRepository.findByInstallationId(INSTALLATION_ID))
        .thenReturn(Optional.of(storedHost()));

    AtlassianHost merged = enricher.update(Optional.of(minimal), Optional.empty()).orElseThrow();

    assertThat(merged.getCloudId())
        .as("Forge invocation cloudId must override the stored row")
        .isEqualTo(CLOUD_FROM_FIT);
  }

  @Test
  void update_keepsSharedSecretFromStoredRow() {
    AtlassianHost minimal = minimalHost(CLOUD_FROM_FIT);
    when(hostByInstallationIdRepository.findByInstallationId(INSTALLATION_ID))
        .thenReturn(Optional.of(storedHost()));

    AtlassianHost merged = enricher.update(Optional.of(minimal), Optional.empty()).orElseThrow();

    assertThat(merged.getSharedSecret()).isEqualTo("db-secret");
  }

  private static AtlassianHost storedHost() {
    AtlassianHost stored = new AtlassianHost();
    stored.setInstallationId(INSTALLATION_ID);
    stored.setClientKey("ck-from-db");
    stored.setBaseUrl("https://tenant.atlassian.net");
    stored.setCloudId("stale-cloud");
    stored.setSharedSecret("db-secret");
    return stored;
  }

  @Test
  void update_returnsMinimalHostWhenInstallationNotInDatabase() {
    AtlassianHost minimal = minimalHost("cloud-1");
    when(hostByInstallationIdRepository.findByInstallationId(INSTALLATION_ID))
        .thenReturn(Optional.empty());

    assertThat(enricher.update(Optional.of(minimal), Optional.empty())).contains(minimal);
  }

  private static AtlassianHost minimalHost(String cloudId) {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId(INSTALLATION_ID);
    host.setCloudId(cloudId);
    host.setClientKey("ck-minimal");
    return host;
  }
}
