package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ContainerExcludedAutoConfigurationsTest {

  @Test
  void applyTo_bindsIndexedExcludeProperties() {
    var environment = new StandardEnvironment();

    ContainerExcludedAutoConfigurations.applyTo(environment);

    assertThat(environment.getProperty("spring.autoconfigure.exclude[0]"))
        .isEqualTo("com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration");
    assertThat(
            environment.getProperty(
                "spring.autoconfigure.exclude["
                    + (ContainerExcludedAutoConfigurations.CLASS_NAMES.size() - 1)
                    + "]"))
        .isEqualTo(
            ContainerExcludedAutoConfigurations.CLASS_NAMES.get(
                ContainerExcludedAutoConfigurations.CLASS_NAMES.size() - 1));
  }

  @Test
  void merge_preservesExistingExcludes() {
    var merged =
        ContainerExcludedAutoConfigurations.merge(
            "com.example.CustomAutoConfiguration", ContainerExcludedAutoConfigurations.CLASS_NAMES);

    assertThat(merged.get(0)).isEqualTo("com.example.CustomAutoConfiguration");
    assertThat(merged)
        .contains("com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration");
  }
}
