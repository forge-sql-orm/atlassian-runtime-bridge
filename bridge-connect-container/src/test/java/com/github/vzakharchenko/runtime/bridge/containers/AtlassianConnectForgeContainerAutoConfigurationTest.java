package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter;
import org.junit.jupiter.api.Test;

class AtlassianConnectForgeContainerAutoConfigurationTest {

  @Test
  void containerAuthorizationFilterRegistersBeforeSpringSecurity() {
    var autoConfig = new AtlassianConnectForgeContainerAutoConfiguration();
    var forgeContextService = mock(ForgeContextService.class);

    var registration =
        autoConfig.containerAuthorizationFilterRegistration(forgeContextService);

    assertThat(registration.getFilter()).isInstanceOf(ContainerAuthorizationFilter.class);
    assertThat(registration.getOrder())
        .as("must run before Spring Security FilterChainProxy (order -100)")
        .isLessThan(-100);
  }
}
