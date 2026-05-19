package sample.connect.spring.atlaskit;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConnectController {
    /**
     * Thymeleaf view for the Connect general page; loads AP and the Atlaskit bundle.
     */
    @GetMapping("/atlaskit")
    public String helloWorld() {
        return "atlaskit";
    }
}
