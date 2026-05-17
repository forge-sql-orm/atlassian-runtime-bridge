package com.github.vzakharchenko.runtime.bridge.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.connect.spring.AtlassianHost;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AtlassianHostContextEnricherTest {

  private static final String CLOUD_ID = "cloud-99";
  private static final String INSTALLATION_ID = "inst-99";
  private static final String CLIENT_KEY_FROM_CONTEXT = "ck-from-ctx";
  private static final String SITE_BASE_URL = "https://site.example";

  @Test
  void passthroughEnricherReturnsSameHostWhenPresent() {
    AtlassianHost host = new AtlassianHost();
    host.setCloudId("cloud-1");
    host.setInstallationId("inst-1");

    AtlassianHostContextEnricher<String> passthrough = (atlassianHost, context) -> atlassianHost;

    assertThat(passthrough.update(Optional.of(host), Optional.of("ctx")))
        .as("default passthrough must keep the optional host")
        .contains(host);
  }

  @Test
  void enricherCanReplaceHostFromContext() {
    AtlassianHost input = new AtlassianHost();
    input.setClientKey("minimal");

    AtlassianHost enriched = new AtlassianHost();
    enriched.setClientKey("enriched-key");
    enriched.setBaseUrl("https://example.atlassian.net");

    AtlassianHostContextEnricher<Void> enricher = (atlassianHost, context) -> Optional.of(enriched);

    assertThat(enricher.update(Optional.of(input), Optional.empty()))
        .as("enricher may return a new host independent of the input instance")
        .contains(enriched);
  }

  @Test
  void enricherReturnsEmptyWhenHostAbsent() {
    AtlassianHostContextEnricher<Object> dropHost = (atlassianHost, context) -> Optional.empty();

    assertThat(dropHost.update(Optional.empty(), Optional.of("context")))
        .as("missing host must not force a synthetic host")
        .isEmpty();
  }

  @Test
  void enricherCanDeriveHostFieldsFromContext() {
    record SiteContext(String baseUrl, String clientKey) {}

    AtlassianHost seed = new AtlassianHost();
    seed.setCloudId(CLOUD_ID);
    seed.setInstallationId(INSTALLATION_ID);

    AtlassianHostContextEnricher<SiteContext> enricher =
        (atlassianHost, context) ->
            context.map(
                site -> {
                  AtlassianHost host = atlassianHost.orElseGet(AtlassianHost::new);
                  host.setBaseUrl(site.baseUrl());
                  host.setClientKey(site.clientKey());
                  return host;
                });

    Optional<AtlassianHost> result =
        enricher.update(
            Optional.of(seed),
            Optional.of(new SiteContext(SITE_BASE_URL, CLIENT_KEY_FROM_CONTEXT)));

    assertThat(result)
        .map(AtlassianHost::getBaseUrl)
        .as("context-driven enrichment must populate baseUrl")
        .contains(SITE_BASE_URL);
  }

  @Test
  void enricherCanPopulateClientKeyFromContext() {
    record SiteContext(String baseUrl, String clientKey) {}

    AtlassianHost seed = new AtlassianHost();
    seed.setCloudId(CLOUD_ID);

    AtlassianHostContextEnricher<SiteContext> enricher =
        (atlassianHost, context) ->
            context.map(
                site -> {
                  AtlassianHost host = atlassianHost.orElseGet(AtlassianHost::new);
                  host.setClientKey(site.clientKey());
                  return host;
                });

    Optional<AtlassianHost> result =
        enricher.update(
            Optional.of(seed), Optional.of(new SiteContext("https://x", CLIENT_KEY_FROM_CONTEXT)));

    assertThat(result.map(AtlassianHost::getClientKey))
        .as("context-driven enrichment must populate clientKey")
        .contains(CLIENT_KEY_FROM_CONTEXT);
  }

  @Test
  void enricherPreservesSeedIdentifiersWhenEnrichingInPlace() {
    record SiteContext(String baseUrl, String clientKey) {}

    AtlassianHost seed = new AtlassianHost();
    seed.setCloudId(CLOUD_ID);
    seed.setInstallationId(INSTALLATION_ID);

    AtlassianHostContextEnricher<SiteContext> enricher =
        (atlassianHost, context) ->
            context.map(
                site -> {
                  AtlassianHost host = atlassianHost.orElseGet(AtlassianHost::new);
                  host.setBaseUrl(site.baseUrl());
                  return host;
                });

    Optional<AtlassianHost> result =
        enricher.update(Optional.of(seed), Optional.of(new SiteContext(SITE_BASE_URL, "ck")));

    assertThat(result.map(AtlassianHost::getCloudId))
        .as("seed identifiers must be preserved when enriching in place")
        .contains(CLOUD_ID);
  }
}
