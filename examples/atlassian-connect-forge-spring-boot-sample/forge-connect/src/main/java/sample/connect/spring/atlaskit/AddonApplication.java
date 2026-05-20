package sample.connect.spring.atlaskit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Connect + Forge sample add-on entry point.
 *
 * <p>{@code bridge-forge-connect} registers via Spring Boot {@code AutoConfiguration.imports}; no
 * need to {@code @ComponentScan} the bridge. This class scans {@code sample.connect.spring.atlaskit}
 * (including shared {@link BusinessLogicController} from {@code core}).
 */
@SpringBootApplication
public class AddonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddonApplication.class, args);
    }
}
