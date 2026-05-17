package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.connect.spring.AtlassianHost;
import org.junit.jupiter.api.Test;

class AtlassianHostMergeTest {

  @Test
  void mergeKeepsSharedSecretFromStoredRow() {
    AtlassianHost merged = AtlassianHostMerge.merge(fullStoredRow(), forgeMinimal());

    assertThat(merged.getSharedSecret()).isEqualTo("db-secret");
  }

  @Test
  void mergeOverlaysCloudIdFromForgeMinimal() {
    AtlassianHost merged = AtlassianHostMerge.merge(fullStoredRow(), forgeMinimal());

    assertThat(merged.getCloudId()).isEqualTo("cloud-from-fit");
  }

  @Test
  void mergeOverlaysClientKeyFromForgeMinimal() {
    AtlassianHost merged = AtlassianHostMerge.merge(fullStoredRow(), forgeMinimal());

    assertThat(merged.getClientKey()).isEqualTo("ck-minimal");
  }

  @Test
  void mergeDoesNotMutatePersistedInstance() {
    AtlassianHost stored = fullStoredRow();
    String originalCloudId = stored.getCloudId();

    AtlassianHostMerge.merge(stored, forgeMinimal());

    assertThat(stored.getCloudId()).isEqualTo(originalCloudId);
  }

  private static AtlassianHost fullStoredRow() {
    AtlassianHost stored = new AtlassianHost();
    stored.setClientKey("ck-from-db");
    stored.setSharedSecret("db-secret");
    stored.setBaseUrl("https://tenant.atlassian.net");
    stored.setCloudId("stale-cloud");
    stored.setInstallationId("ari:cloud:ecosystem::installation/abc");
    stored.setCreatedBy("creator");
    stored.setAddonInstalled(false);
    return stored;
  }

  private static AtlassianHost forgeMinimal() {
    AtlassianHost minimal = new AtlassianHost();
    minimal.setInstallationId("ari:cloud:ecosystem::installation/abc");
    minimal.setCloudId("cloud-from-fit");
    minimal.setClientKey("ck-minimal");
    minimal.setAddonInstalled(true);
    return minimal;
  }
}
