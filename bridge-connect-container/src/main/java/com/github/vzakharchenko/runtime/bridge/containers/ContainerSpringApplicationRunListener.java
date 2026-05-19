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

  public ContainerSpringApplicationRunListener(SpringApplication application, String... args) {}

  @Override
  public void environmentPrepared(
      ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    ContainerExcludedAutoConfigurations.applyTo(environment);
  }
}
