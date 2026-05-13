package sample.connect.spring.atlaskit;

import java.util.Collections;
import java.util.Map;

import com.atlassian.connect.spring.ContextJwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {

    @GetMapping("/atlaskit")
    public String helloWorld() {
        return "atlaskit";
    }

    /**
     * JSON for the Atlaskit iframe (plain {@code fetch} from {@code main.js}).
     * {@link ContextJwt}: accept JWT from {@code AP.context.getToken()} (context token / QSH rules differ from iframe URL JWT).
     */
    @GetMapping("/atlaskit/api/hello")
    @ContextJwt
    @ResponseBody
    public Map<String, String> helloJson() {
        return Collections.singletonMap("message", "Hello from HelloWorldController");
    }
}