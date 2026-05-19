package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ContainerSecurityPropertiesTest {

  @Test
  void defaultExposesHealthOnly() {
    var properties = new ContainerSecurityProperties();

    assertThat(properties.getPublicPaths()).containsExactly("/health");
  }

  @Test
  void overrideViaPropertyReplacesDefaults() {
    var source =
        new MapConfigurationPropertySource(
            Map.of(
                "bridge.container.security.public-paths[0]", "/my-health",
                "bridge.container.security.public-paths[1]", "/api/public/**"));

    var properties =
        new Binder(source).bind("bridge.container.security", ContainerSecurityProperties.class)
            .get();

    assertThat(properties.getPublicPaths()).containsExactly("/my-health", "/api/public/**");
  }

  @Test
  void setterReplacesValueAtomically() {
    var properties = new ContainerSecurityProperties();

    properties.setPublicPaths(List.of("/only"));

    assertThat(properties.getPublicPaths()).containsExactly("/only");
  }
}