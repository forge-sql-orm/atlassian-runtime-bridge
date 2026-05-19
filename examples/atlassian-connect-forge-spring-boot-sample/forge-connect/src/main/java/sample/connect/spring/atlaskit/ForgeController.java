package sample.connect.spring.atlaskit;

import com.atlassian.connect.spring.ForgeRemote;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Forge-facing HTTP handlers invoked through {@link ForgeRemote} (proxied from the Forge app
 * to this Connect backend). Used together with {@code manifest.yml} {@code scheduledTrigger} and
 * {@code trigger} modules so the add-on can refresh Forge system / offline token material on a
 * schedule and after install/upgrade events without duplicating logic for Connect vs Forge UI.
 */
@Controller
public class ForgeController {

    /**
     * Called by Forge when the {@code system-token-sync} remote endpoint is invoked — for example
     * from the {@code scheduledTrigger} (hourly) or {@code trigger} on {@code avi:forge:installed:app}
     * / {@code avi:forge:upgraded:app}. The implementation should perform whatever persistence or
     * token refresh your add-on needs; this sample returns a fixed body only.
     * <p>
     * Path and remote wiring are declared in {@code manifest.yml}: endpoint {@code system-token-sync}
     * maps route {@code /system/sync} onto the {@code connect} remote base URL.
     */
    @PostMapping("/system/sync")
    @ResponseBody
    @ForgeRemote
    public String sync() {
        return "synced";
    }
}
