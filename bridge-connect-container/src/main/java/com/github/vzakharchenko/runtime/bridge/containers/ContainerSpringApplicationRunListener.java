package com.github.vzakharchenko.runtime.bridge.containers;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Re-applies {@link ContainerExcludedAutoConfigurations} at {@code environmentPrepared} so excludes
 * are present before auto-configuration import.
 */
public class ContainerSpringApplicationRunListener implements SpringApplicationRunListener {

  /**
   * Signature dictated by Spring Boot's {@code SpringApplicationRunListener} SPI — the runtime
   * loads listeners by reflectively invoking this exact constructor; the arguments are unused.
   */
  @SuppressWarnings("unused")
  public ContainerSpringApplicationRunListener(SpringApplication application, String... args) {}

  @Override
  public void environmentPrepared(
      ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    ContainerExcludedAutoConfigurations.applyTo(environment);
  }
}
