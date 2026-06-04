package sample.connect.spring.atlaskit;

import java.util.Collections;
import java.util.Map;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ContextJwt;
import com.atlassian.connect.spring.IgnoreJwt;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.common.ManualAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sample.connect.spring.atlaskit.jira.JiraMyselfResponse;

/**
 * Sample web endpoints for the same add-on codebase running as <strong>Connect</strong> (iframe)
 * or as <strong>Forge</strong> (Custom UI + remote). {@link JiraProductAdapter} resolves product
 * calls to the correct transport (Connect JWT vs Forge impersonation / offline token) at runtime.
 */
@Controller
public class BusinessLogicController {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogicController.class);

    private static final String JIRA_REST_MYSELF = "/rest/api/3/myself";

    private static final String INSTALLATION_ID_PREFIX = "ari:cloud:ecosystem::installation/";

    private final JiraProductAdapter jiraProductAdapter;

    private final ManualAuthorizationService manualAuthorizationService;

    public BusinessLogicController(
            JiraProductAdapter jiraProductAdapter,
            ManualAuthorizationService manualAuthorizationService) {
        this.jiraProductAdapter = jiraProductAdapter;
        this.manualAuthorizationService = manualAuthorizationService;
    }

    /**
     * Hello JSON consumed by the bundled UI ({@code fetch} from {@code main.ts} / Forge entry)
     * without changing this controller: the same method serves <strong>Connect</strong> and
     * <strong>Forge</strong> requests; {@link JiraProductAdapter} picks the integration path from context.
     * <p>
     * {@link ContextJwt}: the client must send the JWT from {@code AP.context.getToken()} where
     * applicable (Connect context / QSH rules differ from the iframe URL JWT). Forge-hosted UI
     * relies on the bridge to establish security context before this handler runs.
     */
    @GetMapping("/atlaskit/api/hello")
    @ContextJwt
    @ResponseBody
    public Map<String, String> helloJson(@AuthenticationPrincipal AtlassianHostUser user) {
        JiraMyselfResponse myself = fetchJiraMyself(user);
        String label = myself != null ? myself.displayName() : user.getUserAccountId().orElse("unknown");
        return message("Hello from BusinessLogicController" +
                ". User: " + label);
    }

    /**
     * Forge-oriented impersonation helper: no Connect JWT on the URL ({@link IgnoreJwt}).
     * Host identity comes from query parameters; {@link ManualAuthorizationService} seeds security
     * context so Jira is called as {@code accountId}.
     * <p>
     * This path depends on a valid Forge <strong>offline (system) access token</strong> for the
     * installation (lifetime is typically on the order of <strong>~4 hours</strong>). Refresh it
     * periodically (e.g. scheduler job) or by invoking your remote from Forge Custom UI
     * ({@code forge-remote}) so the backend receives traffic and can persist a fresh token.
     * <p>
     * <strong>Do not expose this pattern in production</strong> without extra controls (network,
     * shared secret, Forge FIT, etc.).
     */
    @GetMapping("/api/impersonation")
    @IgnoreJwt
    @ResponseBody
    public Map<String, String> impersonation(
            @RequestParam("accountId") String accountId,
            @RequestParam("installationId") String installationId,
            @RequestParam("cloudId") String cloudId) {
        LOG.info("Impersonating user: {} installationId: {} cloudId: {}", accountId, installationId, cloudId);
        AtlassianHost host = hostFromQuery(installationId, cloudId);
        try {
            manualAuthorizationService.authorize(host);
            AtlassianHostUser hostUser = AtlassianHostUser.builder(host).withUserAccountId(accountId).build();
            JiraMyselfResponse myself = fetchJiraMyself(hostUser);
            String label = myself != null ? myself.displayName() : accountId;
            return message("Hello from BusinessLogicController. User: " + label);
        } catch (Exception e) {
            LOG.warn("Impersonation failed", e);
            return message("Impersonation failed: " + e.getMessage());
        }
    }

    private JiraMyselfResponse fetchJiraMyself(AtlassianHostUser user) {
        ResponseEntity<JiraMyselfResponse> response =
                jiraProductAdapter.impersonation(user).exchange(JIRA_REST_MYSELF, HttpMethod.GET, null, JiraMyselfResponse.class);
        return response.getBody();
    }

    private static AtlassianHost hostFromQuery(String installationId, String cloudId) {
        AtlassianHost host = new AtlassianHost();
        host.setCloudId(cloudId);
        host.setInstallationId(INSTALLATION_ID_PREFIX + installationId);
        return host;
    }

    private static Map<String, String> message(String text) {
        return Collections.singletonMap("message", text);
    }
}
