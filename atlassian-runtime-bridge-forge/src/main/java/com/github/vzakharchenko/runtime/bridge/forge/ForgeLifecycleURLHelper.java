package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.auth.LifecycleURLHelper;
import com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * No-op {@link LifecycleURLHelper} used in Forge scenarios.
 * <p>
 * When Connect's JPA auto-configuration is not present we still need a
 * {@code LifecycleURLHelper} bean on the classpath, but lifecycle callbacks
 * are handled differently, so all checks always return {@code false}.
 */
@Component
@ConditionalOnMissingBean(AtlassianJpaAutoConfiguration.class)
public class ForgeLifecycleURLHelper extends LifecycleURLHelper {
    public ForgeLifecycleURLHelper() {
        super(null);
    }


    @Override
    public boolean isRequestToLifecycleURL(HttpServletRequest request) {
        return false;
    }

    @Override
    public boolean isRequestToInstalledLifecycle(HttpServletRequest request) {
        return false;
    }

    @Override
    public boolean isRequestToUninstalledLifecycle(HttpServletRequest request) {
        return false;
    }
}
