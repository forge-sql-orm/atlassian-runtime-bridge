package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AtlassianConnectForgeContainerAutoConfigurationTest {

  @Test
  void containerAuthorizationFilterBeanIsCreated() {
    var autoConfig = new AtlassianConnectForgeContainerAutoConfiguration();
    var forgeContextService = mock(ForgeContextService.class);

    var filter = autoConfig.containerAuthorizationFilter(forgeContextService);

    assertThat(filter).isInstanceOf(ContainerAuthorizationFilter.class);
  }

  @Test
  void filterRegistrationIsDisabledToAvoidDoubleRegistration() {
    var autoConfig = new AtlassianConnectForgeContainerAutoConfiguration();
    var filter = autoConfig.containerAuthorizationFilter(mock(ForgeContextService.class));

    var registration = autoConfig.containerAuthorizationFilterRegistration(filter);

    assertThat(registration.isEnabled())
        .as("the filter participates in the Spring Security chain only")
        .isFalse();
    assertThat(registration.getFilter()).isSameAs(filter);
  }
}
