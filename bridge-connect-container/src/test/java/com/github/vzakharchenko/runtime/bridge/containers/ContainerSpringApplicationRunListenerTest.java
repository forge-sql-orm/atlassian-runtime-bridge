package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.core.env.StandardEnvironment;

class ContainerSpringApplicationRunListenerTest {

  @Test
  void environmentPreparedAppliesAutoConfigurationExcludes() {
    var listener =
        new ContainerSpringApplicationRunListener(
            new SpringApplication(StubApp.class), new String[0]);
    var environment = new StandardEnvironment();

    listener.environmentPrepared(new DefaultBootstrapContext(), environment);

    assertThat(environment.getProperty("spring.autoconfigure.exclude[0]"))
        .isEqualTo("com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration");
  }

  @SpringBootConfiguration
  static class StubApp {}
}
