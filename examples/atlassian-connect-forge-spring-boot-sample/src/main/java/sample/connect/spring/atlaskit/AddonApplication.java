package sample.connect.spring.atlaskit;

import com.github.vzakharchenko.runtime.bridge.forge.AtlassianConnectForgeAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@Configuration
@EnableRetry
@ComponentScan(basePackageClasses = {AtlassianConnectForgeAutoConfiguration.class, HelloWorldController.class})
public class AddonApplication {

    public static void main(String[] args) throws Exception {
        new SpringApplication(AddonApplication.class).run(args);
    }
}
