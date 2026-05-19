package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.StandardEnvironment;

class ContainerAutoConfigurationEnvironmentPostProcessorTest {

  private final ContainerAutoConfigurationEnvironmentPostProcessor processor =
      new ContainerAutoConfigurationEnvironmentPostProcessor();

  @Test
  void postProcessEnvironment_appliesContainerExcludes() {
    var environment = new StandardEnvironment();

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("spring.autoconfigure.exclude[0]"))
        .isEqualTo("com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration");
  }

  @Test
  void getOrder_isHighestPrecedence() {
    assertThat(processor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }
}
