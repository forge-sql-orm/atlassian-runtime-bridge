package com.github.vzakharchenko.runtime.bridge.containers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Applies {@link ContainerExcludedAutoConfigurations} when {@code bridge-connect-container} is on
 * the classpath, so consumer apps do not need {@code spring.autoconfigure.exclude} in {@code
 * application.yml}.
 *
 * <p>Registered via {@code
 * META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class ContainerAutoConfigurationEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    ContainerExcludedAutoConfigurations.applyTo(environment);
  }

  /**
   * Run before most other post-processors so {@code spring.autoconfigure.exclude} is visible when
   * auto-configuration classes are selected.
   */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
