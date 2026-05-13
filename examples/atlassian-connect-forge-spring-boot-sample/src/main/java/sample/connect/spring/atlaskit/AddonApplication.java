package sample.connect.spring.atlaskit;

import com.github.vzakharchenko.runtime.bridge.forge.AtlassianConnectForgeAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Connect + Forge sample add-on entry point.
 * <p>
 * Component scanning is anchored on {@link AtlassianConnectForgeAutoConfiguration} so that
 * bridge beans under {@code com.github.vzakharchenko.runtime.bridge.*} are registered together
 * with this sample package.
 */
@SpringBootApplication
@EnableRetry
@ComponentScan(basePackageClasses = {AtlassianConnectForgeAutoConfiguration.class, HelloWorldController.class})
public class AddonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddonApplication.class, args);
    }
}
