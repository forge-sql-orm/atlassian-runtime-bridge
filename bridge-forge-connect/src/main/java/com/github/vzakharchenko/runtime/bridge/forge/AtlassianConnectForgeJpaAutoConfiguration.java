package com.github.vzakharchenko.runtime.bridge.forge;

import com.github.vzakharchenko.runtime.bridge.forge.persistence.AtlassianHostByInstallationIdRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers bridge JPA repositories alongside Connect's {@code AtlassianJpaAutoConfiguration}.
 *
 * <p>Connect already enables {@link com.atlassian.connect.spring.AtlassianHostRepository} and
 * related stores; this configuration adds {@link AtlassianHostByInstallationIdRepository} only,
 * without {@code @Primary} or changes to Connect beans.
 *
 * <p>Listed in {@code AutoConfiguration.imports}; active only when Connect JPA is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(
    name = "com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration")
@AutoConfigureAfter(
    name = "com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration")
@EnableJpaRepositories(basePackageClasses = AtlassianHostByInstallationIdRepository.class)
public class AtlassianConnectForgeJpaAutoConfiguration {}
