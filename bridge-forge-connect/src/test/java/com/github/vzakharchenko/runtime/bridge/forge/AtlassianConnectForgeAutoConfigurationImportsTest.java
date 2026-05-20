package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AtlassianConnectForgeAutoConfigurationImportsTest {

  @Test
  void autoConfigurationImports_listsForgeBridgeConfigurations() throws Exception {
    String resource =
        "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
    try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
      assertThat(in).as("AutoConfiguration.imports must be on the classpath").isNotNull();
      String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(contents)
          .contains(AtlassianConnectForgeAutoConfiguration.class.getName())
          .contains(AtlassianConnectForgeJpaAutoConfiguration.class.getName());
    }
  }
}
