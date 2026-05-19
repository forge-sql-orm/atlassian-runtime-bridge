package com.github.vzakharchenko.runtime.bridge.containers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration class names vetoed in Forge Container runtime (same effect as {@code
 * spring.autoconfigure.exclude} in application config).
 */
final class ContainerExcludedAutoConfigurations {

  static final String PROPERTY = "spring.autoconfigure.exclude";

  static final String PROPERTY_SOURCE_NAME = "bridgeConnectContainerAutoConfigurationExcludes";

  static final List<String> CLASS_NAMES =
      List.of(
          // Connect Spring Boot
          "com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration",
          "com.atlassian.connect.spring.internal.AtlassianConnectWebMvcAutoConfiguration",
          "com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration",
          // JDBC / JPA / migrations
          "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
          "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
          "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
          "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
          "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
          "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
          // Unused in typical container deployments
          "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration",
          "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
          "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");

  private ContainerExcludedAutoConfigurations() {}

  /** Binds {@link #CLASS_NAMES} as indexed {@value #PROPERTY} entries (highest precedence). */
  static void applyTo(ConfigurableEnvironment environment) {
    List<String> merged = merge(environment.getProperty(PROPERTY), CLASS_NAMES);
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int i = 0; i < merged.size(); i++) {
      properties.put(PROPERTY + "[" + i + "]", merged.get(i));
    }
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
  }

  /**
   * Merges comma-separated {@value #PROPERTY} from config with {@code defaults}, preserving order
   * and deduplicating.
   */
  static List<String> merge(String existingCsv, List<String> defaults) {
    List<String> merged = new ArrayList<>();
    if (StringUtils.hasText(existingCsv)) {
      for (String token : StringUtils.commaDelimitedListToStringArray(existingCsv)) {
        String trimmed = token.trim();
        if (!trimmed.isEmpty() && !merged.contains(trimmed)) {
          merged.add(trimmed);
        }
      }
    }
    for (String className : defaults) {
      if (!merged.contains(className)) {
        merged.add(className);
      }
    }
    return merged;
  }
}
