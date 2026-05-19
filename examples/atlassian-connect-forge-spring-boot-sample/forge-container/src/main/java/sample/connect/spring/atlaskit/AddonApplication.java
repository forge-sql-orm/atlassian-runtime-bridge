package sample.connect.spring.atlaskit;

import com.github.vzakharchenko.runtime.bridge.containers.AtlassianConnectForgeContainerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Forge Container sample entry point (egress proxy + ingress headers, no Connect iframe/JWT).
 *
 * <p>Component scan anchors on {@link AtlassianConnectForgeContainerAutoConfiguration} so
 * {@code bridge-connect-container} beans load together with {@link BusinessLogicController}.
 *
 * <p>Connect/JPA auto-configuration is excluded by {@link
 * com.github.vzakharchenko.runtime.bridge.containers.ContainerAutoConfigurationEnvironmentPostProcessor}
 * when {@code bridge-connect-container} is on the classpath. Do not add {@code
 * atlassian-connect-spring-boot-jpa-starter} or {@code bridge-forge-connect} to this module (see
 * {@code pom.xml}).
 */
@SpringBootApplication
@ComponentScan(
        basePackageClasses = {
                AtlassianConnectForgeContainerAutoConfiguration.class,
                BusinessLogicController.class,
                AddonApplication.class
        })
public class AddonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddonApplication.class, args);
    }
}
