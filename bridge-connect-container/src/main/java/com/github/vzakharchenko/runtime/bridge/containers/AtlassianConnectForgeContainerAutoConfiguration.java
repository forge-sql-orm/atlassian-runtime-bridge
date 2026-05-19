package com.github.vzakharchenko.runtime.bridge.containers;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for the Forge Containers runtime: scans the bridge {@code common}
 * API package and the {@code containers} package so the egress client, ingress filters, Forge
 * context service, and product adapters get registered without consumer-side
 * {@code @ComponentScan}.
 *
 * <p>Registered via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. Atlassian
 * Connect/JPA auto-configurations are suppressed via {@link
 * ContainerAutoConfigurationEnvironmentPostProcessor} ({@code spring.autoconfigure.exclude}).
 */
@AutoConfiguration
@ComponentScan("com.github.vzakharchenko.runtime.bridge.common")
@ComponentScan("com.github.vzakharchenko.runtime.bridge.containers")
public class AtlassianConnectForgeContainerAutoConfiguration {}
