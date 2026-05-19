package com.github.vzakharchenko.runtime.bridge.containers;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for {@link ContainerWebSecurityConfiguration}. {@code publicPaths} lists request
 * matchers that bypass authentication; override via {@code bridge.container.security.public-paths}
 * in {@code application.yml} when an app exposes health/liveness on different endpoints than the
 * defaults.
 */
@ConfigurationProperties(prefix = "bridge.container.security")
public class ContainerSecurityProperties {

  private List<String> publicPaths =
      List.of( "/health");

  public List<String> getPublicPaths() {
    return publicPaths;
  }

  public void setPublicPaths(List<String> publicPaths) {
    this.publicPaths = publicPaths;
  }
}
